import { ActivityListPage } from './ActivityListPage'

export default function ActivitiesPage() {
  return <ActivityListPage title="All Activities" description="Admin view of all records across the system" defaultFilters={{ scope: 'all' }} showStatus />
}
