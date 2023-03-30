package fr.openobservatory.backend.dto;

import fr.openobservatory.backend.entities.UserEntity;
import lombok.Data;

@Data
public class UserDto {

  private String username;
  private String avatar;
  private boolean isPublic;
  private UserEntity.Type type;
}
