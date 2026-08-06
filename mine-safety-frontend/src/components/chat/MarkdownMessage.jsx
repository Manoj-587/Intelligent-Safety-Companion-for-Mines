import React from 'react';
import ReactMarkdown from 'react-markdown';
import Typography from '@mui/material/Typography';

const MarkdownMessage = ({ content }) => {
  return (
    <Typography
      component="div"
      sx={{
        wordBreak: 'break-word',
        '& p': {
          margin: '0 0 0.75rem',
          lineHeight: 1.7
        },
        '& strong': {
          fontWeight: 700
        },
        '& em': {
          fontStyle: 'italic'
        },
        '& ul, & ol': {
          pl: 3,
          mb: 1,
          lineHeight: 1.7
        },
        '& code': {
          backgroundColor: 'rgba(15, 36, 60, 0.06)',
          borderRadius: 2,
          fontFamily: 'Inter, monospace',
          padding: '2px 6px'
        },
        '& a': {
          color: 'secondary.main',
          textDecoration: 'underline'
        }
      }}
    >
      <ReactMarkdown>{content}</ReactMarkdown>
    </Typography>
  );
};

export default MarkdownMessage;
