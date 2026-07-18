import { ActivityListPage } from './ActivityListPage'

export default function PendingPage() {
  return <ActivityListPage title="Pending Records" description="Records awaiting completion" defaultFilters={{ work_status: 'not_finished' }} />
}
