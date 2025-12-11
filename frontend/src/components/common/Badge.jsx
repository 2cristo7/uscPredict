const variants = {
  default: 'bg-[#2a2a2a] text-[#a0a0a0]',
  success: 'bg-[#22c55e]/20 text-[#22c55e]',
  danger: 'bg-[#ef4444]/20 text-[#ef4444]',
  warning: 'bg-[#f59e0b]/20 text-[#f59e0b]',
  info: 'bg-[#3b82f6]/20 text-[#3b82f6]',
  purple: 'bg-[#8b5cf6]/20 text-[#8b5cf6]',
};

const sizes = {
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-2.5 py-1 text-xs',
  lg: 'px-3 py-1.5 text-sm',
};

// Status mappings for common use cases
const statusVariants = {
  // Event status
  DRAFT: 'default',
  OPEN: 'success',
  CLOSED: 'danger',
  RESOLVED: 'info',
  CANCELLED: 'danger',
  // Market status
  ACTIVE: 'success',
  SUSPENDED: 'warning',
  SETTLED: 'info',
  // Order status
  PENDING: 'warning',
  PARTIALLY_FILLED: 'info',
  FILLED: 'success',
  // User roles
  ADMIN: 'purple',
  USER: 'default',
  // Transaction types
  DEPOSIT: 'success',
  WITHDRAWAL: 'danger',
  ORDER_PLACED: 'info',
  ORDER_EXECUTED: 'success',
  ORDER_CANCELLED: 'warning',
  SETTLEMENT: 'purple',
};

const Badge = ({
  children,
  variant = 'default',
  size = 'md',
  status,
  className = '',
}) => {
  // If status is provided, use the status mapping
  const resolvedVariant = status ? (statusVariants[status] || 'default') : variant;

  return (
    <span
      className={`
        inline-flex items-center font-medium rounded-full
        ${variants[resolvedVariant]}
        ${sizes[size]}
        ${className}
      `}
    >
      {children || status}
    </span>
  );
};

export default Badge;
