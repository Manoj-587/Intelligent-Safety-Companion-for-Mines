import { useState } from 'react';

export const useDeveloperMode = () => {
  const [enabled, setEnabled] = useState(false);
  return { enabled, toggle: () => setEnabled((current) => !current) };
};
