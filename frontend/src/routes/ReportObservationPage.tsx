import { faSave } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { useNavigate } from '@tanstack/react-location';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';

import { celestialBodies } from '~/api';
import { CreateObservationData, create, visibilityOptions } from '~/api/observations';
import celeste from '~/assets/png/celeste.png';
import { Button, DatePicker, Map, Select, TextInput, Title } from '~/components';
import { MarkerInput } from '~/components/molecules/MarkerInput';
import { registerAdapter as r } from '~/utils';

function ReportObservationPage() {
  const { data: celestialBodiesData } = useQuery({
    queryFn: celestialBodies.getAll,
    queryKey: ['celestial-bodies'],
  });
  const { handleSubmit, register, setValue, watch } = useForm<CreateObservationData>();
  const navigate = useNavigate();
  const { isLoading, mutate } = useMutation({
    mutationFn: create,
    onSuccess: (o) => navigate({ to: `/observations/${o.id}` }),
  });
  const selectedBody = watch('celestialBodyId') ?? -1;
  return (
    <>
      <Title as="h2" className="mt-4 mb-2" centered>
        Création d&apos;une observation
      </Title>
      <img
        className="object-cover h-64 w-full"
        src={celestialBodiesData?.data.find((c) => c.id === +selectedBody)?.image ?? celeste}
        alt="Objet céleste"
      />
      <form onSubmit={handleSubmit((data) => mutate(data))}>
        <div className="bg-slate-600">
          <div className="flex flex-col gap-y-4 max-w-screen-sm mx-auto py-4 w-3/4">
            <Select
              options={celestialBodiesData?.data.map((c) => ({ name: c.name, value: `${c.id}` })) ?? []}
              placeholder="Objet céleste observé"
              {...register('celestialBodyId', { required: true })}
            />
            <DatePicker aria-label="Date" placeholder="Date" {...register('timestamp', { required: true })} />
            <TextInput
              aria-label="Degré d'orientation"
              placeholder="Degré d'orientation"
              {...r(register, 'orientation', {
                required: true,
                validate: (o) =>
                  /\d+/.test(o.toString()) ? undefined : "Le degré d'orientation doit être une valeur en degrés.",
              })}
            />
            <Select
              options={visibilityOptions}
              placeholder="Visibilité de l'observation"
              {...register('visibility', { required: true })}
            />
          </div>
        </div>
        <Map className="h-64">
          <MarkerInput
            onMove={(p) => {
              setValue('lat', p.lat);
              setValue('lng', p.lng);
            }}
          />
        </Map>
        <div className="bg-slate-600 flex justify-center py-8">
          <Button isDisabled={isLoading} type="submit">
            Enregistrer
            <FontAwesomeIcon className="ml-20" icon={faSave} />
          </Button>
        </div>
      </form>
    </>
  );
}

export default ReportObservationPage;
