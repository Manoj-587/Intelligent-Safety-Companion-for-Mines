import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Chip from '@mui/material/Chip';

const DeveloperPanel = ({
  intent,
  persona,
  templateUsed,
  knowledgeDocCount,
  conversationTurnCount,
  totalPromptChars,
  promptTruncated,
  provider,
  latencyMs,
  riskLevel,
  policy,
  blocked
}) => {
  const stats = [
    { label: 'Risk Level', value: riskLevel || 'HIGH' },
    { label: 'Policy', value: policy || 'PASS_WITH_WARNING' },
    { label: 'Blocked', value: String(blocked ?? 'false') },
    { label: 'Intent', value: intent || 'n/a' },
    { label: 'Persona', value: persona || 'n/a' },
    { label: 'Template', value: templateUsed || 'n/a' },
    { label: 'Knowledge docs', value: String(knowledgeDocCount ?? 0) },
    { label: 'Turns', value: String(conversationTurnCount ?? 0) },
    { label: 'Prompt chars', value: String(totalPromptChars ?? 0) },
    { label: 'Truncated', value: promptTruncated ? 'Yes' : 'No' },
    { label: 'Provider', value: provider || 'openrouter' },
    { label: 'Latency', value: `${latencyMs ?? 0} ms` }
  ];

  return (
    <Paper elevation={2} sx={{ p: 3.25, display: 'grid', gap: 2, bgcolor: 'grey.50' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
        <Typography variant="h6" sx={{ fontWeight: 800 }}>
          Developer Diagnostics
        </Typography>
        <Chip label="Developer mode" color="secondary" size="small" />
      </Box>

      <Typography variant="body2" color="text.secondary">
        Developer diagnostics are shown only when developer mode is enabled.
      </Typography>

      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 1.25 }}>
        {stats.map((stat) => (
          <Paper key={stat.label} sx={{ p: 1.5, bgcolor: 'white', borderRadius: 3, boxShadow: 'none' }}>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', letterSpacing: 0.5 }}>
              {stat.label}
            </Typography>
            <Typography variant="body2" sx={{ fontWeight: 700, mt: 0.5 }}>
              {stat.value}
            </Typography>
          </Paper>
        ))}
      </Box>
    </Paper>
  );
};

export default DeveloperPanel;
