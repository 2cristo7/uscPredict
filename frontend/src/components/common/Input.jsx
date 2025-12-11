import { forwardRef } from 'react';

const Input = forwardRef(({
  label,
  error,
  type = 'text',
  placeholder,
  className = '',
  inputClassName = '',
  ...props
}, ref) => {
  return (
    <div className={`${className}`}>
      {label && (
        <label className="block text-sm font-medium text-[#a0a0a0] mb-2">
          {label}
        </label>
      )}
      <input
        ref={ref}
        type={type}
        placeholder={placeholder}
        className={`
          w-full px-4 py-2.5 rounded-lg
          bg-[#0a0a0a] border border-[#2a2a2a]
          text-white placeholder-[#666666]
          focus:outline-none focus:border-[#3b82f6] focus:ring-1 focus:ring-[#3b82f6]
          transition-all duration-150
          disabled:opacity-50 disabled:cursor-not-allowed
          ${error ? 'border-[#ef4444] focus:border-[#ef4444] focus:ring-[#ef4444]' : ''}
          ${inputClassName}
        `}
        {...props}
      />
      {error && (
        <p className="mt-1.5 text-sm text-[#ef4444]">{error}</p>
      )}
    </div>
  );
});

Input.displayName = 'Input';

export const Textarea = forwardRef(({
  label,
  error,
  placeholder,
  rows = 3,
  className = '',
  ...props
}, ref) => {
  return (
    <div className={`${className}`}>
      {label && (
        <label className="block text-sm font-medium text-[#a0a0a0] mb-2">
          {label}
        </label>
      )}
      <textarea
        ref={ref}
        rows={rows}
        placeholder={placeholder}
        className={`
          w-full px-4 py-2.5 rounded-lg
          bg-[#0a0a0a] border border-[#2a2a2a]
          text-white placeholder-[#666666]
          focus:outline-none focus:border-[#3b82f6] focus:ring-1 focus:ring-[#3b82f6]
          transition-all duration-150 resize-none
          disabled:opacity-50 disabled:cursor-not-allowed
          ${error ? 'border-[#ef4444] focus:border-[#ef4444] focus:ring-[#ef4444]' : ''}
        `}
        {...props}
      />
      {error && (
        <p className="mt-1.5 text-sm text-[#ef4444]">{error}</p>
      )}
    </div>
  );
});

Textarea.displayName = 'Textarea';

export const Select = forwardRef(({
  label,
  error,
  options = [],
  placeholder,
  className = '',
  ...props
}, ref) => {
  return (
    <div className={`${className}`}>
      {label && (
        <label className="block text-sm font-medium text-[#a0a0a0] mb-2">
          {label}
        </label>
      )}
      <select
        ref={ref}
        className={`
          w-full px-4 py-2.5 rounded-lg
          bg-[#0a0a0a] border border-[#2a2a2a]
          text-white
          focus:outline-none focus:border-[#3b82f6] focus:ring-1 focus:ring-[#3b82f6]
          transition-all duration-150
          disabled:opacity-50 disabled:cursor-not-allowed
          ${error ? 'border-[#ef4444] focus:border-[#ef4444] focus:ring-[#ef4444]' : ''}
        `}
        {...props}
      >
        {placeholder && (
          <option value="" className="text-[#666666]">{placeholder}</option>
        )}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && (
        <p className="mt-1.5 text-sm text-[#ef4444]">{error}</p>
      )}
    </div>
  );
});

Select.displayName = 'Select';

export default Input;
