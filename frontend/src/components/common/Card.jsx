const Card = ({
  children,
  title,
  subtitle,
  footer,
  onClick,
  hoverable = false,
  className = '',
  padding = 'md',
}) => {
  const paddingClasses = {
    none: '',
    sm: 'p-4',
    md: 'p-6',
    lg: 'p-8',
  };

  return (
    <div
      onClick={onClick}
      className={`
        bg-[#1a1a1a] border border-[#2a2a2a] rounded-xl
        ${hoverable ? 'hover:bg-[#1f1f1f] hover:border-[#333333] cursor-pointer' : ''}
        transition-all duration-150
        ${paddingClasses[padding]}
        ${className}
      `}
    >
      {(title || subtitle) && (
        <div className={`${padding !== 'none' ? '' : 'p-6'} ${children ? 'mb-4' : ''}`}>
          {title && (
            <h3 className="text-lg font-semibold text-white">{title}</h3>
          )}
          {subtitle && (
            <p className="text-sm text-[#a0a0a0] mt-1">{subtitle}</p>
          )}
        </div>
      )}
      {children}
      {footer && (
        <div className={`${padding !== 'none' ? '' : 'p-6'} pt-4 mt-4 border-t border-[#2a2a2a]`}>
          {footer}
        </div>
      )}
    </div>
  );
};

export default Card;
