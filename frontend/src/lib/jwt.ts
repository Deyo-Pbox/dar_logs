export interface JwtPayload {
  sub: number
  username: string
  role: 'admin' | 'user'
  exp: number
  iat: number
}

export function decodeJwt(token: string): JwtPayload {
  const payload = token.split('.')[1]
  if (!payload) throw new Error('Invalid JWT token')
  return JSON.parse(atob(payload))
}
