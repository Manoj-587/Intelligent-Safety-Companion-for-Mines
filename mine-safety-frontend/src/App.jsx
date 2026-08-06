import Box from '@mui/material/Box';
import AppShell from './components/layout/AppShell';
import ErrorBoundary from './components/ErrorBoundary';

function App() {
  return (
    <ErrorBoundary>
      <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
        <AppShell />
      </Box>
    </ErrorBoundary>
  );
}

export default App;
