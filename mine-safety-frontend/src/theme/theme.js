import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1f3752',
      light: '#466591',
      dark: '#0f243d',
      contrastText: '#ffffff'
    },
    secondary: {
      main: '#f2a900',
      light: '#ffdb4d',
      dark: '#b27700',
      contrastText: '#1f2632'
    },
    success: {
      main: '#2e7d32'
    },
    warning: {
      main: '#f7b500'
    },
    error: {
      main: '#d32f2f'
    },
    background: {
      default: '#eef2f6',
      paper: '#ffffff'
    },
    text: {
      primary: '#19232d',
      secondary: '#5b6b80'
    }
  },
  typography: {
    fontFamily: ['Inter', 'Roboto', 'sans-serif'].join(','),
    h1: {
      fontSize: '2.25rem',
      fontWeight: 800
    },
    h2: {
      fontSize: '1.9rem',
      fontWeight: 800
    },
    h3: {
      fontSize: '1.55rem',
      fontWeight: 700
    },
    h4: {
      fontSize: '1.25rem',
      fontWeight: 700
    },
    body1: {
      fontSize: '1rem'
    },
    body2: {
      fontSize: '0.95rem'
    }
  },
  spacing: 8,
  shape: {
    borderRadius: 18
  },
  components: {
    MuiPaper: {
      styleOverrides: {
        root: {
          borderRadius: 20,
          boxShadow: '0 18px 48px rgba(15, 30, 60, 0.08)'
        }
      }
    },
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 14,
          padding: '12px 18px'
        }
      }
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          fontWeight: 700
        }
      }
    }
  }
});

export default theme;
