import { Component } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Paper from '@mui/material/Paper';

class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      return (
        <Box sx={{ p: 4, display: 'flex', justifyContent: 'center' }}>
          <Paper sx={{ p: 4, maxWidth: 520, width: '100%', borderRadius: 3, textAlign: 'center' }} elevation={3}>
            <Typography variant="h5" sx={{ mb: 2, fontWeight: 800 }}>
              Oops! Something interrupted the app.
            </Typography>
            <Typography variant="body1" sx={{ mb: 3, color: 'text.secondary' }}>
              We hit an unexpected error. Please retry or refresh the page to continue.
            </Typography>
            <Button variant="contained" color="primary" onClick={this.handleRetry}>
              Retry
            </Button>
          </Paper>
        </Box>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
