import { createRouter, createWebHistory } from 'vue-router'

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
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/admin/permissions',
      children: [
        { path: 'permissions', name: 'Permissions', component: () => import('@/views/admin/PermissionsView.vue') },
        { path: 'ai-models', name: 'AIModels', component: () => import('@/views/admin/AIModelsView.vue') },
        { path: 'audit', name: 'Audit', component: () => import('@/views/admin/AuditView.vue') },
      ],
    },
    {
      path: '/candidate',
      component: () => import('@/layouts/MobileLayout.vue'),
      children: [
        { path: '', name: 'JobList', component: () => import('@/views/candidate/JobListView.vue') },
        { path: 'job', name: 'JobDetail', component: () => import('@/views/candidate/JobDetailView.vue') },
        { path: 'apply', name: 'Apply', component: () => import('@/views/candidate/ApplyView.vue') },
        { path: 'applications', name: 'Applications', component: () => import('@/views/candidate/ApplicationsView.vue') },
        { path: 'resume', name: 'Resume', component: () => import('@/views/candidate/ResumeView.vue') },
        { path: 'profile', name: 'Profile', component: () => import('@/views/candidate/ProfileView.vue') },
      ],
    },
    {
      path: '/interviewer',
      component: () => import('@/layouts/InterviewerLayout.vue'),
      children: [
        { path: '', name: 'InterviewList', component: () => import('@/views/interviewer/InterviewListView.vue') },
        { path: 'detail', name: 'InterviewDetail', component: () => import('@/views/interviewer/InterviewDetailView.vue') },
        { path: 'ai-mode', name: 'AIMode', component: () => import('@/views/interviewer/AIModeView.vue') },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

export default router
