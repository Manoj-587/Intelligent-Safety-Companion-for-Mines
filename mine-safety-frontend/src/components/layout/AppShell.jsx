import { useMemo, useState } from 'react';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Grid from '@mui/material/Grid';
import Sidebar from './Sidebar';
import ChatPanel from '../chat/ChatPanel';
import RiskSummaryCard from '../dashboard/RiskSummaryCard';
import RecommendationPanel from '../dashboard/RecommendationPanel';
import DeveloperPanel from '../developer/DeveloperPanel';

const roleMap = {
  Worker: 'WORKER',
  Supervisor: 'SUPERVISOR',
  'Maintenance Engineer': 'MAINTENANCE',
  'Safety Officer': 'SAFETY_OFFICER'
};

const AppShell = () => {
  const [selectedRole, setSelectedRole] = useState('Worker');
  const [developerEnabled, setDeveloperEnabled] = useState(false);
  const [developerDiagnostics, setDeveloperDiagnostics] = useState(null);

  const backendRole = useMemo(() => roleMap[selectedRole], [selectedRole]);

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', bgcolor: 'linear-gradient(180deg, #eef2f6 0%, #ffffff 100%)' }}>
      <Sidebar
        selectedRole={selectedRole}
        onRoleChange={setSelectedRole}
        developerEnabled={developerEnabled}
        onToggleDeveloper={() => setDeveloperEnabled((value) => !value)}
      />
      <Box component="main" sx={{ flex: 1, p: { xs: 2, md: 3 }, bgcolor: 'background.default' }}>
        <Container maxWidth="xl" disableGutters>
          <Grid container spacing={3}>
            <Grid item xs={12} md={8}>
              <ChatPanel
                roleLabel={selectedRole}
                backendRole={backendRole}
                developerEnabled={developerEnabled}
                onDiagnosticsUpdate={setDeveloperDiagnostics}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <Box sx={{ display: 'grid', gap: 3 }}>
                <RiskSummaryCard />
                <RecommendationPanel />
                {developerEnabled && (
                  <DeveloperPanel
                    intent={developerDiagnostics?.intent ?? 'n/a'}
                    persona={developerDiagnostics?.persona ?? 'n/a'}
                    templateUsed={developerDiagnostics?.templateUsed ?? 'n/a'}
                    knowledgeDocCount={developerDiagnostics?.knowledgeDocCount ?? 0}
                    conversationTurnCount={developerDiagnostics?.conversationTurnCount ?? 0}
                    totalPromptChars={developerDiagnostics?.totalPromptChars ?? 0}
                    promptTruncated={developerDiagnostics?.promptTruncated ?? false}
                    provider={developerDiagnostics?.provider ?? 'unknown'}
                    latencyMs={developerDiagnostics?.latencyMs ?? 0}
                  />
                )}
              </Box>
            </Grid>
          </Grid>
        </Container>
      </Box>
    </Box>
  );
};

export default AppShell;
