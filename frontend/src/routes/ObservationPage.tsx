import { Button, Map, Text } from '@/components';
import { useMatch } from '@tanstack/react-location';
import celest from '@/assets/png/celeste.png';
import iconUser from '@/assets/png/icon-user.png';
import clsx from 'clsx';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowDown, faArrowUp, faPen } from '@fortawesome/free-solid-svg-icons';
import { Marker, Popup } from 'react-leaflet';

export default function ObservationPage(): JSX.Element {
  const {
    params: { id },
  } = useMatch<{ Params: { id: string } }>();
  const expired = true;
  const isUser = true;
  return (
    <div className="md:flex md:w-full">
      <div className="w-full">
        <div className={clsx('w-full h-1/3 relative', 'md:h-[50vh]')}>
          {expired && (
            <Text as="span" color="black" centered className="absolute top-5 left-5 py-2 px-5 rounded-[45px] bg-white">
              Expirée
            </Text>
          )}
          {isUser && (
            <Button
              unstyled
              className="bg-white absolute top-4 right-2 w-14 md:w-16 md:h-16 h-14 flex justify-center items-center rounded-[45px]"
            >
              <FontAwesomeIcon icon={faPen} size="xl" />
            </Button>
          )}
          <img src={celest} alt="image de l'observation" className="h-full w-full" />
          <div className={clsx('absolute bottom-1 right-2  flex-col justify-around items-center')}>
            <div className="w-10 flex justify-center items-center">
              <Button unstyled>
                <FontAwesomeIcon icon={faArrowUp} size="2xl" color="white" />
              </Button>
            </div>
            <Text as="p" color="white" centered className={clsx('w-10 py-1')}>
              10
            </Text>
            <div className="w-10 flex justify-center items-center">
              <Button unstyled>
                <FontAwesomeIcon icon={faArrowDown} size="2xl" color="grey" />
              </Button>
            </div>
          </div>
        </div>
        <div className={clsx('w-full h-1/4 bg-slate-500 pb-5', 'md:h-[50vh]')}>
          <div className="pt-4 pl-3 flex justify-between">
            <div className="w-1/2">
              <Text as="h1" bold className="text-lg md:text-2xl">
                Galaxie Messier
              </Text>
              <Text as="p" className={clsx('text-xs', 'md:text-base')}>
                le 22/11/2022 à 18h44
              </Text>
            </div>
            <div className="flex items-center pr-4 md:pr-10">
              <img src={iconUser} className={clsx('w-10 md:w-12')} />
              <Text as="span" color="white" className="pl-2 md:pl-4">
                Utilisateur
              </Text>
            </div>
          </div>
          <div className="md:mt-10 px-4 mt-3">
            <Text as="span" color="white" bold className="px-1">
              Visibilité :
            </Text>
            <Text as="span" color="white">
              À l&apos;oeil nu
            </Text>
          </div>
          <div className="md:mt-5 px-4 mt-3">
            <Text as="span" color="white" bold className="px-1">
              Orientation :
            </Text>
            <Text as="span" color="white">
              98°
            </Text>
          </div>
          <div className="md:mt-5 px-4 mt-3">
            <Text as="span" color="white" bold className="px-1">
              Description :
            </Text>
            <Text as="span" color="white">
              L&apos;observation est somptueuse.
            </Text>
          </div>
        </div>
      </div>
      <Map
        className="h-[calc(100vh-20rem)] md:h-[100vh] w-full"
        center={[49.2332, 1.813]}
        noFly
        withoutNotificationCircle
      >
        <Marker position={[49.2332, 1.813]}>
          <Popup>Nom de l&apos;observation</Popup>
        </Marker>
      </Map>
    </div>
  );
}
