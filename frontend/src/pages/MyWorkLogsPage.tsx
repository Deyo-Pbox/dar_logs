import { ActivityListPage } from './ActivityListPage'

export default function MyWorkLogsPage() {
  return <ActivityListPage title="My Work Logs" description="Records you have created" defaultFilters={{ scope: 'user' }} />
}
