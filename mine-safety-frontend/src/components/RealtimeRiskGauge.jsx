import { motion } from 'framer-motion';
import { AlertTriangle, Shield } from 'lucide-react';

const RealtimeRiskGauge = ({ riskLevel, riskScore, className = '' }) => {
  const getColor = () => {
    if (riskLevel === 'CRITICAL') return '#e05252';
    if (riskLevel === 'WARNING') return '#f5a623';
    return '#3ecf8e';
  };

  const getIcon = () => {
    if (riskLevel === 'CRITICAL') return AlertTriangle;
    return Shield;
  };

  const Icon = getIcon();

  const circumference = 2 * Math.PI * 60;
  const strokeDashoffset = circumference - (riskScore / 100) * circumference;

  return (
    <motion.div 
      className={`p-6 rounded-2xl bg-gradient-to-br from-gray-900/50 to-gray-800/50 backdrop-blur-xl border border-white/10 ${className}`}
      whileHover={{ scale: 1.05, rotate: [0, 5, -5, 0] }}
      transition={{ duration: 0.3 }}
    >
      <div className="relative">
        <svg className="w-32 h-32 mx-auto" viewBox="0 0 140 140">
          <circle
            cx="70"
            cy="70"
            r="60"
            fill="none"
            stroke="rgba(255,255,255,0.1)"
            strokeWidth="12"
          />
          <motion.circle
            cx="70"
            cy="70"
            r="60"
            fill="none"
            stroke={getColor()}
            strokeWidth="12"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            pathLength={1}
            initial={{ pathLength: 0 }}
            animate={{ pathLength: 1 }}
            transition={{ duration: 2, ease: 'easeOut' }}
          />
          <motion.text
            x="70"
            y="80"
            textAnchor="middle"
            className="text-3xl font-bold fill-current text-white"
            style={{ fontFamily: 'Inter' }}
            animate={{ scale: [1, 1.1, 1] }}
            transition={{ duration: 2, repeat: Infinity }}
          >
            {Math.round(riskScore || 0)}
          </motion.text>
        </svg>
        <div className="absolute -bottom-8 left-1/2 transform -translate-x-1/2">
          <div className={`w-16 h-16 rounded-full ${riskLevel === 'CRITICAL' ? 'bg-red-500/20 border-4 border-red-500/50 animate-pulse' : 'bg-green-500/20 border-4 border-green-500/50' } flex items-center justify-center shadow-2xl`}>
            <Icon className={`w-8 h-8 ${riskLevel === 'CRITICAL' ? 'text-red-400' : 'text-green-400'}`} />
          </div>
        </div>
      </div>
      <div className="text-center mt-4">
        <h3 className="text-xl font-bold text-white capitalize">{riskLevel}</h3>
        <p className="text-gray-400 text-sm">Risk Level</p>
      </div>
    </motion.div>
  );
};

export default RealtimeRiskGauge;

