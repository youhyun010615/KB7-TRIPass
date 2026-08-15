import axios from './index';

export const registerFcmToken = (token) => {
  return axios.post('/notifications/tokens', {
    deviceToken: token,
    deviceType: 'WEB'
  });
};
