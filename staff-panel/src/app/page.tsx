import { redirect } from 'next/navigation'

// Root redirects to /dashboard (which is inside the authenticated layout)
export default function Root() {
  redirect('/dashboard')
}
