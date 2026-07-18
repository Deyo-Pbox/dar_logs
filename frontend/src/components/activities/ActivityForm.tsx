import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { ActivityLog, ActivityPayload } from '@/api/types'
import { useMunicipalities } from '@/hooks/useDashboard'
import { Loader2 } from 'lucide-react'

const activitySchema = z.object({
  municipality: z.string().min(1, 'Municipality is required'),
  lo_claimant: z.string().optional(),
  title_no: z.string().optional(),
  odts_no: z.string().optional(),
  lot_no: z.string().optional(),
  survey_no: z.string().optional(),
  area_has: z.string().optional(),
  location: z.string().optional(),
  transmitted_documents: z.string().optional(),
  received_by_control_no: z.string().optional(),
  remarks_action_taken: z.string().optional(),
  work_status: z.enum(['not_finished', 'finished']).optional(),
})

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (data: ActivityPayload) => void
  record?: ActivityLog | null
  loading?: boolean
}

export function ActivityForm({ open, onOpenChange, onSubmit, record, loading }: Props) {
  const { data: muniData } = useMunicipalities()
  const municipalities: string[] = muniData?.municipalities ?? muniData?.data ?? []

  const form = useForm({
    defaultValues: {
      municipality: record?.municipality ?? '',
      lo_claimant: record?.lo_claimant ?? '',
      title_no: record?.title_no ?? '',
      odts_no: record?.odts_no ?? '',
      lot_no: record?.lot_no ?? '',
      survey_no: record?.survey_no ?? '',
      area_has: record?.area_has != null ? String(record.area_has) : '',
      location: record?.location ?? '',
      transmitted_documents: record?.transmitted_documents ?? '',
      received_by_control_no: record?.received_by_control_no ?? '',
      remarks_action_taken: record?.remarks_action_taken ?? '',
      work_status: (record?.work_status ?? 'not_finished') as 'not_finished' | 'finished',
    },
  })
  const { register, handleSubmit, setValue, watch, reset, formState: { errors } } = form

  const validate = (data: Record<string, unknown>) => {
    const result = activitySchema.safeParse(data)
    if (!result.success) {
      result.error.issues.forEach((issue) => {
        form.setError(issue.path[0] as keyof typeof form.formState.errors, { message: issue.message })
      })
      return
    }
    const d = result.data
    onSubmit({
      municipality: d.municipality,
      lo_claimant: d.lo_claimant || undefined,
      title_no: d.title_no || undefined,
      odts_no: d.odts_no || undefined,
      lot_no: d.lot_no || undefined,
      survey_no: d.survey_no || undefined,
      area_has: d.area_has ? Number(d.area_has) : undefined,
      location: d.location || undefined,
      transmitted_documents: d.transmitted_documents || undefined,
      received_by_control_no: d.received_by_control_no || undefined,
      remarks_action_taken: d.remarks_action_taken || undefined,
      work_status: d.work_status,
    })
  }

  const selectedMuni = watch('municipality')

  return (
    <Dialog open={open} onOpenChange={(v) => { if (!v) reset(); onOpenChange(v) }}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{record ? 'Edit Activity' : 'New Activity'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(validate)} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2 col-span-2 sm:col-span-1">
              <Label htmlFor="municipality">Municipality *</Label>
              {municipalities.length > 0 ? (
                <Select value={selectedMuni} onValueChange={(v) => setValue('municipality', v)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select..." />
                  </SelectTrigger>
                  <SelectContent>
                    {municipalities.map((m) => (
                      <SelectItem key={m} value={m}>{m}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              ) : (
                <Input id="municipality" {...register('municipality')} />
              )}
              {errors.municipality && <p className="text-xs text-destructive">{errors.municipality.message}</p>}
            </div>
            <div className="space-y-2 col-span-2 sm:col-span-1">
              <Label htmlFor="work_status">Status</Label>
              <Select
                value={watch('work_status')}
                onValueChange={(v) => setValue('work_status', v as 'not_finished' | 'finished')}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="not_finished">Pending</SelectItem>
                  <SelectItem value="finished">Done</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="lo_claimant">Claimant</Label>
              <Input id="lo_claimant" {...register('lo_claimant')} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="title_no">Title No.</Label>
              <Input id="title_no" {...register('title_no')} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="odts_no">ODTS No.</Label>
              <Input id="odts_no" {...register('odts_no')} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="lot_no">Lot No.</Label>
              <Input id="lot_no" {...register('lot_no')} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="survey_no">Survey No.</Label>
              <Input id="survey_no" {...register('survey_no')} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="area_has">Area (ha)</Label>
              <Input id="area_has" type="number" step="0.0001" {...register('area_has')} />
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="location">Location</Label>
            <Input id="location" {...register('location')} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="transmitted_documents">Transmitted Documents</Label>
            <Input id="transmitted_documents" {...register('transmitted_documents')} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="received_by_control_no">Control No.</Label>
            <Input id="received_by_control_no" {...register('received_by_control_no')} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="remarks_action_taken">Remarks</Label>
            <Input id="remarks_action_taken" {...register('remarks_action_taken')} />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={loading}>
              {loading && <Loader2 className="h-4 w-4 animate-spin mr-2" />}
              {record ? 'Save Changes' : 'Create Record'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
