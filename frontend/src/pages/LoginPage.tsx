import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useAuthStore } from '@/stores/authStore'
import { login, register } from '@/api/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { toast } from 'sonner'
import { Loader2, UserPlus, LogIn } from 'lucide-react'

const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
})

const registerSchema = z.object({
  username: z.string().min(3, 'At least 3 characters'),
  password: z.string().min(6, 'At least 6 characters'),
  confirmPassword: z.string(),
}).refine((d) => d.password === d.confirmPassword, {
  message: 'Passwords must match',
  path: ['confirmPassword'],
})

type LoginForm = z.infer<typeof loginSchema>
type RegisterForm = z.infer<typeof registerSchema>

export default function LoginPage() {
  const { token, isExpired, setAuth } = useAuthStore()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [loading, setLoading] = useState(false)

  const loginForm = useForm<LoginForm>({ resolver: zodResolver(loginSchema) })
  const registerForm = useForm<RegisterForm>({ resolver: zodResolver(registerSchema) })

  if (token && !isExpired()) return <Navigate to="/" replace />

  const handleLogin = async (data: LoginForm) => {
    setLoading(true)
    try {
      const res = await login(data.username, data.password)
      setAuth(res.token, res.user)
      toast.success('Welcome back!')
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Login failed'
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  const handleRegister = async (data: RegisterForm) => {
    setLoading(true)
    try {
      await register(data.username, data.password)
      toast.success('Registration submitted. Awaiting admin approval.')
      setMode('login')
      registerForm.reset()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Registration failed'
      toast.error(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary/10 via-background to-accent/10 p-4">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <img src="./assets/images/dar-logo-white-bg.png" alt="DAR Logo" className="mx-auto h-16 w-auto" />
          <h1 className="mt-4 text-2xl font-bold">DAR Activity Logs</h1>
          <p className="text-sm text-muted-foreground">Department of Agrarian Reform</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>{mode === 'login' ? 'Sign In' : 'Create Account'}</CardTitle>
            <CardDescription>
              {mode === 'login'
                ? 'Enter your credentials to access the system'
                : 'Register a new account (requires admin approval)'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {mode === 'login' ? (
              <form onSubmit={loginForm.handleSubmit(handleLogin)} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="username">Username</Label>
                  <Input id="username" {...loginForm.register('username')} autoComplete="username" />
                  {loginForm.formState.errors.username && (
                    <p className="text-xs text-destructive">{loginForm.formState.errors.username.message}</p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="password">Password</Label>
                  <Input id="password" type="password" {...loginForm.register('password')} autoComplete="current-password" />
                  {loginForm.formState.errors.password && (
                    <p className="text-xs text-destructive">{loginForm.formState.errors.password.message}</p>
                  )}
                </div>
                <Button type="submit" className="w-full" disabled={loading}>
                  {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <LogIn className="h-4 w-4" />}
                  Sign In
                </Button>
              </form>
            ) : (
              <form onSubmit={registerForm.handleSubmit(handleRegister)} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="reg-username">Username</Label>
                  <Input id="reg-username" {...registerForm.register('username')} autoComplete="username" />
                  {registerForm.formState.errors.username && (
                    <p className="text-xs text-destructive">{registerForm.formState.errors.username.message}</p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="reg-password">Password</Label>
                  <Input id="reg-password" type="password" {...registerForm.register('password')} autoComplete="new-password" />
                  {registerForm.formState.errors.password && (
                    <p className="text-xs text-destructive">{registerForm.formState.errors.password.message}</p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="reg-confirm">Confirm Password</Label>
                  <Input id="reg-confirm" type="password" {...registerForm.register('confirmPassword')} autoComplete="new-password" />
                  {registerForm.formState.errors.confirmPassword && (
                    <p className="text-xs text-destructive">{registerForm.formState.errors.confirmPassword.message}</p>
                  )}
                </div>
                <Button type="submit" className="w-full" disabled={loading}>
                  {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <UserPlus className="h-4 w-4" />}
                  Create Account
                </Button>
              </form>
            )}

            <div className="mt-4 text-center text-sm">
              {mode === 'login' ? (
                <>
                  Don't have an account?{' '}
                  <button type="button" onClick={() => setMode('register')} className="font-medium text-primary hover:underline">
                    Register
                  </button>
                </>
              ) : (
                <>
                  Already have an account?{' '}
                  <button type="button" onClick={() => setMode('login')} className="font-medium text-primary hover:underline">
                    Sign In
                  </button>
                </>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
