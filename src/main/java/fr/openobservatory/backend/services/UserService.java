package fr.openobservatory.backend.services;

import fr.openobservatory.backend.dto.*;
import fr.openobservatory.backend.entities.UserEntity;
import fr.openobservatory.backend.exceptions.*;
import fr.openobservatory.backend.repositories.ObservationRepository;
import fr.openobservatory.backend.repositories.PushSubscriptionRepository;
import fr.openobservatory.backend.repositories.UserAchievementRepository;
import fr.openobservatory.backend.repositories.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class UserService {

  private final ModelMapper modelMapper;
  private final ObservationRepository observationRepository;
  private final PasswordEncoder passwordEncoder;
  private final PushSubscriptionRepository pushSubscriptionRepository;
  private final UserRepository userRepository;
  private final UserAchievementRepository userAchievementRepository;

  // ---

  public UserWithProfileDto create(CreateUserDto dto) {
    if (!Pattern.matches(UserEntity.USERNAME_PATTERN, dto.getUsername()))
      throw new InvalidUsernameException();
    if (userRepository.existsByUsernameIgnoreCase(dto.getUsername()))
      throw new UsernameAlreadyUsedException();
    var entity = new UserEntity();
    entity.setUsername(dto.getUsername());
    entity.setPassword(passwordEncoder.encode(dto.getPassword()));
    entity.setBiography(dto.getBiography());
    entity.setType(UserEntity.Type.USER);
    entity.setPublic(true);
    entity.setCreatedAt(Instant.now());
    entity.setNotificationsEnabled(false);
    entity.setRadius(5);
    var userDto = modelMapper.map(userRepository.save(entity), UserWithProfileDto.class);
    userDto.setAchievements(Set.of());
    userDto.setKarma(0);
    return userDto;
  }

  public UserWithProfileDto findByUsername(String username, String issuerUsername) {
    var issuer =
        issuerUsername != null
            ? userRepository
                .findByUsernameIgnoreCase(issuerUsername)
                .orElseThrow(UnavailableUserException::new)
            : null;
    var user =
        userRepository.findByUsernameIgnoreCase(username).orElseThrow(UnknownUserException::new);
    if (!isViewableBy(user, issuer)) throw new UserNotVisibleException();
    var dto = modelMapper.map(user, UserWithProfileDto.class);
    dto.setAchievements(
        userAchievementRepository.findAllByUser(user).stream()
            .map(a -> modelMapper.map(a, AchievementDto.class))
            .collect(Collectors.toSet()));
    dto.setKarma(getKarma(user));
    return dto;
  }

  public List<ObservationWithDetailsDto> findObservationsByUsername(
      String username, String issuerUsername) {
    var issuer =
        issuerUsername != null
            ? userRepository
                .findByUsernameIgnoreCase(issuerUsername)
                .orElseThrow(UnavailableUserException::new)
            : null;
    var user =
        userRepository.findByUsernameIgnoreCase(username).orElseThrow(UnknownUserException::new);
    if (!isViewableBy(user, issuer)) throw new UserNotVisibleException();
    return observationRepository.findAllByAuthor(user, Pageable.ofSize(100)).stream()
        .map(
            o -> {
              var dto = modelMapper.map(o, ObservationWithDetailsDto.class);
              dto.setExpired(
                  o.getCreatedAt()
                      .plus(o.getCelestialBody().getValidityTime(), ChronoUnit.HOURS)
                      .isBefore(Instant.now()));
              return dto;
            })
        .toList();
  }

  public SelfUserDto findSelf(String issuerUsername) {
    var dto =
        userRepository
            .findByUsernameIgnoreCase(issuerUsername)
            .map(u -> modelMapper.map(u, SelfUserDto.class))
            .orElseThrow(UnavailableUserException::new);
    dto.setAchievements(
        userAchievementRepository
            .findAllByUser(
                userRepository
                    .findByUsernameIgnoreCase(issuerUsername)
                    .orElseThrow(UnknownUserException::new))
            .stream()
            .map(a -> modelMapper.map(a, AchievementDto.class))
            .collect(Collectors.toSet()));
    dto.setKarma(
        getKarma(
            userRepository
                .findByUsernameIgnoreCase(issuerUsername)
                .orElseThrow(UnknownUserException::new)));
    return dto;
  }

  public void modifyPassword(String username, ChangePasswordDto dto, String issuerUsername) {
    var issuer =
        userRepository
            .findByUsernameIgnoreCase(issuerUsername)
            .orElseThrow(UnavailableUserException::new);
    var user =
        userRepository.findByUsernameIgnoreCase(username).orElseThrow(UnknownUserException::new);
    if (!isEditableBy(user, issuer)) throw new UserNotEditableException();
    if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
      throw new PasswordMismatchException();
    }
    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
    userRepository.save(user);
  }

  public SelfUserDto update(String username, UpdateProfileDto dto, String issuerUsername) {
    var issuer =
        userRepository
            .findByUsernameIgnoreCase(issuerUsername)
            .orElseThrow(UnavailableUserException::new);
    var user =
        userRepository.findByUsernameIgnoreCase(username).orElseThrow(UnknownUserException::new);
    if (!isEditableBy(user, issuer)) throw new UserNotEditableException();
    if (dto.getBiography().isPresent()) {
      var biography = dto.getBiography().get();
      user.setBiography(biography);
    }
    if (dto.getAvatar().isPresent()) {
      user.setAvatar(dto.getAvatar().get());
    }
    if (dto.getIsPublic().isPresent()) {
      user.setPublic(dto.getIsPublic().get());
    }
    if (dto.getRadius().isPresent()) {
      user.setRadius(dto.getRadius().get());
    }
    if (dto.getNotificationsEnabled().isPresent()) {
      user.setNotificationsEnabled(dto.getNotificationsEnabled().get());
      if (!user.isNotificationsEnabled()) pushSubscriptionRepository.deleteAllByUser(user);
    }
    var userDto = modelMapper.map(userRepository.save(user), SelfUserDto.class);
    userDto.setAchievements(
        userAchievementRepository.findAllByUser(user).stream()
            .map(a -> modelMapper.map(a, AchievementDto.class))
            .collect(Collectors.toSet()));
    userDto.setKarma(getKarma(user));
    return userDto;
  }

  // ---

  private boolean isEditableBy(UserEntity targetedUser, UserEntity issuer) {
    return issuer.getType().equals(UserEntity.Type.ADMIN) || targetedUser.equals(issuer);
  }

  private boolean isViewableBy(UserEntity targetedUser, UserEntity issuer) {
    return (issuer != null && issuer.getType().equals(UserEntity.Type.ADMIN))
        || targetedUser.isPublic()
        || targetedUser.equals(issuer);
  }

  public void saveUserPosition(String username, UpdatePositionDto dto, String issuerUsername) {
    var issuer =
        userRepository
            .findByUsernameIgnoreCase(issuerUsername)
            .orElseThrow(UnavailableUserException::new);
    var user =
        userRepository.findByUsernameIgnoreCase(username).orElseThrow(UnknownUserException::new);
    if (!isEditableBy(user, issuer)) throw new UserNotEditableException();
    user.setLastPositionUpdate(Instant.now());
    user.setLatitude(dto.getLatitude());
    user.setLongitude(dto.getLongitude());
    userRepository.save(user);
  }

  public int getKarma(UserEntity user) {
    var observations = observationRepository.findAllByAuthor(user);
    return observations.stream()
        .map(o -> o.getVotes().stream().map(v -> v.getVote().getWeight()).reduce(0, Integer::sum))
        .reduce(0, Integer::sum);
  }

  public void delete(String username) {
    var user =
        userRepository.findByUsernameIgnoreCase(username).orElseThrow(UnknownUserException::new);
    userRepository.delete(user);
  }
}
