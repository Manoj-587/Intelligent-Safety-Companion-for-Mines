import { apiClient } from './api';

export const sendChatRequest = async (payload) => {
  const response = await apiClient.post('/api/chat', payload);
  return response.data;
};
