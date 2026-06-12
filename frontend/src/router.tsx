import { createRootRoute, createRoute, createRouter } from '@tanstack/react-router'
import RootLayout from './components/RootLayout'
import HomePage from './pages/HomePage'
import HealthPage from './pages/HealthPage'

const rootRoute = createRootRoute({ component: RootLayout })

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: HomePage,
})

const healthRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/health',
  component: HealthPage,
})

const routeTree = rootRoute.addChildren([indexRoute, healthRoute])

export const router = createRouter({ routeTree })

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
