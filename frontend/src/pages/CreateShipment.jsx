import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { api } from '../api/client.js'

export default function CreateShipment() {
  const navigate = useNavigate()
  const { register, handleSubmit, formState: { errors } } = useForm()

  const { data: facilities = [] } = useQuery({
    queryKey: ['facilities'],
    queryFn: api.listFacilities,
  })

  const mutation = useMutation({
    mutationFn: api.createShipment,
    onSuccess: (shipment) => navigate(`/shipments/${shipment.id}`),
  })

  const onSubmit = (form) => {
    mutation.mutate({
      sender: { name: form.senderName, street: form.senderStreet, city: form.senderCity, state: form.senderState, zip: form.senderZip },
      receiver: { name: form.receiverName, street: form.receiverStreet, city: form.receiverCity, state: form.receiverState, zip: form.receiverZip },
      weightKg: Number(form.weightKg),
      serviceLevel: form.serviceLevel,
      originFacilityId: form.originFacilityId,
      destinationFacilityId: form.destinationFacilityId,
    })
  }

  return (
    <div className="card">
      <h2>Create Shipment</h2>
      <form className="stack" onSubmit={handleSubmit(onSubmit)}>
        <fieldset>
          <legend>Sender</legend>
          <label>Name
            <input {...register('senderName', { required: true })} />
          </label>
          {errors.senderName && <span className="error">Required</span>}
          <label>Street<input {...register('senderStreet', { required: true })} /></label>
          <label>City<input {...register('senderCity', { required: true })} /></label>
          <label>State<input {...register('senderState', { required: true })} /></label>
          <label>Zip<input {...register('senderZip', { required: true })} /></label>
        </fieldset>

        <fieldset>
          <legend>Receiver</legend>
          <label>Name<input {...register('receiverName', { required: true })} /></label>
          <label>Street<input {...register('receiverStreet', { required: true })} /></label>
          <label>City<input {...register('receiverCity', { required: true })} /></label>
          <label>State<input {...register('receiverState', { required: true })} /></label>
          <label>Zip<input {...register('receiverZip', { required: true })} /></label>
        </fieldset>

        <label>Weight (kg)
          <input type="number" step="0.1" {...register('weightKg', { required: true, min: 0.1 })} />
        </label>
        {errors.weightKg && <span className="error">Enter a positive weight</span>}

        <label>Service level
          <select {...register('serviceLevel', { required: true })}>
            <option value="GROUND">Ground</option>
            <option value="AIR">Air</option>
            <option value="EXPRESS">Express</option>
          </select>
        </label>

        <label>Origin facility
          <select {...register('originFacilityId', { required: true })}>
            <option value="">Select…</option>
            {facilities.map((f) => <option key={f.id} value={f.id}>{f.name}</option>)}
          </select>
        </label>

        <label>Destination facility
          <select {...register('destinationFacilityId', { required: true })}>
            <option value="">Select…</option>
            {facilities.map((f) => <option key={f.id} value={f.id}>{f.name}</option>)}
          </select>
        </label>

        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creating…' : 'Create Shipment'}
        </button>
        {mutation.isError && <span className="error">{mutation.error.message}</span>}
      </form>
    </div>
  )
}
