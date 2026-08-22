import axios from './index';

export const getNotifications = () => axios.get('/notifications');
export const markNotificationRead = (id) =>
  axios.patch(`/notifications/${id}/read`);
export const markAllNotificationsRead = () =>
  axios.patch('/notifications/read-all');

export const getSettings = () => axios.get('/notifications/settings');
export const updateSettings = (settings) =>
  axios.put('/notifications/settings', settings);
