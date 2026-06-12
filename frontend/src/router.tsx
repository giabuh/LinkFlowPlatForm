import { createRootRoute, createRoute, createRouter, Outlet, redirect } from '@tanstack/react-router'
import AppShell from './components/AppShell'
import LoginPage from './pages/LoginPage'
import OverviewPage from './pages/OverviewPage'
import LinksPage from './pages/LinksPage'
import UsersPage from './pages/UsersPage'
import { useAuthStore } from './stores/auth'

const rootRoute = createRootRoute({ component: () => <Outlet /> })

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/login',
  component: LoginPage,
  beforeLoad: () => {
    if (useAuthStore.getState().token) throw redirect({ to: '/' })
  },
})

const appRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: 'app',
  component: AppShell,
  beforeLoad: () => {
    if (!useAuthStore.getState().token) throw redirect({ to: '/login' })
  },
})

const indexRoute = createRoute({ getParentRoute: () => appRoute, path: '/', component: OverviewPage })
const linksRoute = createRoute({ getParentRoute: () => appRoute, path: '/links', component: LinksPage })
const usersRoute = createRoute({ getParentRoute: () => appRoute, path: '/users', component: UsersPage })

const routeTree = rootRoute.addChildren([
  loginRoute,
  appRoute.addChildren([indexRoute, linksRoute, usersRoute]),
])

export const router = createRouter({ routeTree })

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
