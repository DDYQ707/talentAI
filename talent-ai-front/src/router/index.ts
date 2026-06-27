import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore, type PortalRole } from '@/stores/auth'

const PUBLIC_PATHS = new Set(['/login', '/register'])

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/RegisterView.vue'),
    },
    {
      path: '/hr',
      meta: { requiresAuth: true, portalRole: 'hr' as PortalRole },
      component: () => import('@/layouts/HRLayout.vue'),
      children: [
        { path: '', name: 'HRWorkbench', component: () => import('@/views/hr/HRWorkbenchView.vue') },
        { path: 'ai-assistant', name: 'AIAssistant', component: () => import('@/views/hr/AIAssistantView.vue') },
        { path: 'jobs', name: 'JobsManagement', component: () => import('@/views/hr/JobsManagementView.vue') },
        { path: 'resumes', name: 'ResumeManagement', component: () => import('@/views/hr/ResumeManagementView.vue') },
        { path: 'resumes/detail', name: 'ResumeDetail', component: () => import('@/views/hr/ResumeDetailView.vue') },
        { path: 'interviews', name: 'InterviewManagement', component: () => import('@/views/hr/InterviewManagementView.vue') },
        { path: 'offers', name: 'OfferManagement', component: () => import('@/views/hr/OfferManagementView.vue') },
        { path: 'talent-pool', name: 'TalentPool', component: () => import('@/views/hr/TalentPoolView.vue') },
        { path: 'dashboard', name: 'RecruitDashboard', component: () => import('@/views/hr/RecruitDashboardView.vue') },
      ],
    },
    {
      path: '/admin',
      meta: { requiresAuth: true, portalRole: 'admin' as PortalRole },
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/admin/dashboard',
      children: [
        { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/DashboardView.vue') },
        { path: 'broadcast', name: 'BroadcastConsole', component: () => import('@/views/admin/BroadcastView.vue') },
        { path: 'permissions', name: 'Permissions', component: () => import('@/views/admin/PermissionsView.vue') },
        { path: 'accounts', name: 'AdminAccounts', component: () => import('@/views/admin/AccountManagementView.vue') },
        { path: 'ai-models', name: 'AIModels', component: () => import('@/views/admin/AIModelsView.vue') },
        // { path: 'audit', name: 'Audit', component: () => import('@/views/admin/AuditView.vue') },
        { path: 'enterprise-audit', name: 'EnterpriseAudit', component: () => import('@/views/admin/EnterpriseAuditView.vue') },
        { path: 'data-dict', name: 'DataDict', component: () => import('@/views/admin/DataDictView.vue') },
        { path: 'job-risk', name: 'JobRisk', component: () => import('@/views/admin/JobRiskView.vue') },
      ],
    },
    {
      path: '/candidate',
      meta: { requiresAuth: true, portalRole: 'candidate' as PortalRole },
      component: () => import('@/layouts/MobileLayout.vue'),
      children: [
        { path: '', name: 'JobList', component: () => import('@/views/candidate/JobListView.vue') },
        { path: 'job', name: 'JobDetail', component: () => import('@/views/candidate/JobDetailView.vue') },
        { path: 'apply', name: 'Apply', component: () => import('@/views/candidate/ApplyView.vue') },
        { path: 'applications', name: 'Applications', component: () => import('@/views/candidate/ApplicationsView.vue') },
        { path: 'offer', name: 'CandidateOffer', component: () => import('@/views/candidate/CandidateOfferView.vue') },
        { path: 'interviews', name: 'CandidateInterviews', component: () => import('@/views/candidate/InterviewsView.vue') },
        { path: 'interview', name: 'CandidateInterviewDetail', component: () => import('@/views/candidate/InterviewDetailView.vue') },
        { path: 'resume', name: 'Resume', component: () => import('@/views/candidate/ResumeView.vue') },
        { path: 'resume/edit', name: 'ResumeEdit', component: () => import('@/views/candidate/ResumeEditView.vue') },
        { path: 'profile', name: 'Profile', component: () => import('@/views/candidate/ProfileView.vue') },
        { path: 'profile/edit', name: 'ProfileEdit', component: () => import('@/views/candidate/ProfileEditView.vue') },
        { path: 'notifications', name: 'Notifications', component: () => import('@/views/candidate/NotificationsView.vue') },
        { path: 'favorites', name: 'Favorites', component: () => import('@/views/candidate/FavoritesView.vue') },
      ],
    },
    {
      path: '/interviewer',
      meta: { requiresAuth: true, portalRole: 'interviewer' as PortalRole },
      component: () => import('@/layouts/InterviewerLayout.vue'),
      redirect: '/interviewer/workbench',
      children: [
        { path: 'workbench', name: 'InterviewerWorkbench', component: () => import('@/views/interviewer/InterviewerWorkbenchView.vue') },
        { path: 'interviews', name: 'InterviewList', component: () => import('@/views/interviewer/InterviewListView.vue') },
        { path: 'detail', name: 'InterviewDetail', component: () => import('@/views/interviewer/InterviewDetailView.vue') },
        { path: 'prep', name: 'InterviewPrep', component: () => import('@/views/interviewer/InterviewPrepView.vue') },
        { path: 'notifications', name: 'InterviewerNotifications', component: () => import('@/views/interviewer/InterviewerNotificationsView.vue') },
        { path: 'notes', redirect: (to) => ({ path: '/interviewer/prep', query: to.query }) },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const loggedIn = auth.isLoggedIn()

  if (PUBLIC_PATHS.has(to.path)) {
    if (loggedIn && to.path === '/login') {
      const accountRole = auth.userInfo?.userType != null
        ? auth.portalRoleFromUserType(auth.userInfo.userType)
        : null
      if (accountRole) {
        return auth.pathForRole(accountRole)
      }
    }
    return true
  }

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth === true)
  if (requiresAuth && !loggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const requiredRole = [...to.matched]
    .reverse()
    .find((record) => record.meta.portalRole)?.meta.portalRole as PortalRole | undefined

  if (requiredRole && loggedIn && auth.userInfo?.userType != null) {
    const accountRole = auth.portalRoleFromUserType(auth.userInfo.userType)
    if (accountRole && accountRole !== requiredRole) {
      return auth.pathForRole(accountRole)
    }
  }

  return true
})

export default router
