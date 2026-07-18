import { ActivityListPage } from './ActivityListPage'

export default function CompletedPage() {
  return <ActivityListPage title="Completed Records" description="Records marked as finished" defaultFilters={{ work_status: 'finished' }} />
}
