import { useState } from 'react';

const Tabs = ({
  tabs,
  defaultTab,
  onChange,
  className = '',
  tabClassName = '',
}) => {
  const [activeTab, setActiveTab] = useState(defaultTab || tabs[0]?.id);

  const handleTabClick = (tabId) => {
    setActiveTab(tabId);
    onChange?.(tabId);
  };

  const activeTabData = tabs.find((tab) => tab.id === activeTab);

  return (
    <div className={className}>
      {/* Tab Headers */}
      <div className="flex border-b border-[#2a2a2a]">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => handleTabClick(tab.id)}
            disabled={tab.disabled}
            className={`
              px-4 py-3 text-sm font-medium transition-all duration-150
              border-b-2 -mb-px
              ${
                activeTab === tab.id
                  ? 'text-white border-[#3b82f6]'
                  : 'text-[#a0a0a0] border-transparent hover:text-white hover:border-[#333333]'
              }
              ${tab.disabled ? 'opacity-50 cursor-not-allowed' : ''}
              ${tabClassName}
            `}
          >
            {tab.icon && <span className="mr-2">{tab.icon}</span>}
            {tab.label}
            {tab.count !== undefined && (
              <span className="ml-2 px-2 py-0.5 text-xs rounded-full bg-[#2a2a2a]">
                {tab.count}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      <div className="pt-4">
        {activeTabData?.content}
      </div>
    </div>
  );
};

// Alternative: Controlled tabs component
export const TabList = ({ tabs, activeTab, onChange, className = '' }) => {
  return (
    <div className={`flex border-b border-[#2a2a2a] ${className}`}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          onClick={() => onChange(tab.id)}
          disabled={tab.disabled}
          className={`
            px-4 py-3 text-sm font-medium transition-all duration-150
            border-b-2 -mb-px
            ${
              activeTab === tab.id
                ? 'text-white border-[#3b82f6]'
                : 'text-[#a0a0a0] border-transparent hover:text-white hover:border-[#333333]'
            }
            ${tab.disabled ? 'opacity-50 cursor-not-allowed' : ''}
          `}
        >
          {tab.icon && <span className="mr-2">{tab.icon}</span>}
          {tab.label}
          {tab.count !== undefined && (
            <span className="ml-2 px-2 py-0.5 text-xs rounded-full bg-[#2a2a2a]">
              {tab.count}
            </span>
          )}
        </button>
      ))}
    </div>
  );
};

// Pill-style tabs (alternative style)
export const PillTabs = ({ tabs, activeTab, onChange, className = '' }) => {
  return (
    <div className={`inline-flex bg-[#1a1a1a] rounded-lg p-1 ${className}`}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          onClick={() => onChange(tab.id)}
          disabled={tab.disabled}
          className={`
            px-4 py-2 text-sm font-medium rounded-md transition-all duration-150
            ${
              activeTab === tab.id
                ? 'bg-[#3b82f6] text-white'
                : 'text-[#a0a0a0] hover:text-white'
            }
            ${tab.disabled ? 'opacity-50 cursor-not-allowed' : ''}
          `}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
};

export default Tabs;
