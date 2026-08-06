import { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { sendChatRequest } from '../services/chatService';

const initialMessages = [
  {
    id: 'system-1',
    role: 'assistant',
    content: 'Welcome to the Intelligent Safety Companion. How can I assist you with mine safety today?',
    timestamp: new Date().toISOString(),
    status: 'normal'
  }
];

export const useChat = (userRole) => {
  const [messages, setMessages] = useState(initialMessages);
  const [isTyping, setIsTyping] = useState(false);
  const [status, setStatus] = useState('normal');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [connectionStatus, setConnectionStatus] = useState('connecting');
  const [developerDiagnostics, setDeveloperDiagnostics] = useState(null);
  const autoScrollRef = useRef(null);
  const sessionId = 'local-session';

  useEffect(() => {
    setConnectionStatus('connecting');
    setConnectionStatus('connected');
  }, []);

  useEffect(() => {
    autoScrollRef.current?.scrollTo({ top: autoScrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, isTyping]);

  const sendMessage = async (text) => {
    setError(null);
    if (!text.trim()) return;

    const userMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: text,
      timestamp: new Date().toISOString(),
      status: 'normal'
    };

    setMessages((prev) => [...prev, userMessage]);
    setIsTyping(true);
    setIsLoading(true);

    try {
      const payload = { sessionId, message: text, role: userRole };
      const requestStart = performance.now();
      const response = await sendChatRequest(payload);
      const requestEnd = performance.now();
      const latencyMs = Math.round(response.latencyMs ?? (requestEnd - requestStart));

      const HARD_BLOCK_POLICIES = ['PromptInjectionPolicy', 'RoleAuthorizationPolicy', 'SensorHallucinationPolicy', 'SystemPromptLeakPolicy'];
      const hardBlocked = response.blocked && HARD_BLOCK_POLICIES.includes(response.diagnostics?.blockedBy);
      const assistantStatus = hardBlocked ? 'blocked' : response.degraded ? 'degraded' : 'normal';
      setStatus(assistantStatus);

      const diagnostics = response.diagnostics;
      setDeveloperDiagnostics({
        intent: response.intent,
        persona: diagnostics?.personaUsed ?? 'n/a',
        templateUsed: diagnostics?.templateUsed ?? 'n/a',
        knowledgeDocCount: diagnostics?.knowledgeDocCount ?? 0,
        conversationTurnCount: diagnostics?.conversationTurnCount ?? 0,
        totalPromptChars: diagnostics?.totalPromptChars ?? 0,
        promptTruncated: diagnostics?.promptTruncated ?? false,
        provider: response.provider ?? 'openrouter',
        latencyMs,
        riskLevel: response.riskLevel || 'HIGH',
        policy: diagnostics?.blockedBy || 'HighRiskPriorityPolicy (PASS_WITH_WARNING)',
        blocked: response.blocked ? 'true' : 'false'
      });

      setMessages((prev) => [
        ...prev,
        {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          content: response.reply,
          timestamp: new Date().toISOString(),
          status: assistantStatus
        }
      ]);
      setConnectionStatus('connected');
    } catch (caught) {
      setConnectionStatus('offline');
      if (axios.isAxiosError(caught) && caught.code === 'ECONNABORTED') {
        setError('The request timed out. Please try again later.');
      } else {
        setError('Unable to connect to the backend. Please check your connection.');
      }
    } finally {
      setIsTyping(false);
      setIsLoading(false);
    }
  };

  const setAutoScrollRef = (element) => {
    autoScrollRef.current = element;
  };

  return {
    messages,
    isTyping,
    status,
    isLoading,
    error,
    connectionStatus,
    developerDiagnostics,
    sendMessage,
    setAutoScrollRef
  };
};
