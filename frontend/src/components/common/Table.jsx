import Spinner from './Spinner';

const Table = ({
  columns,
  data,
  loading = false,
  emptyMessage = 'No data available',
  onRowClick,
  rowKey = 'id',
  className = '',
  stickyHeader = true,
}) => {
  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Spinner size="md" />
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="flex items-center justify-center py-12 text-[#a0a0a0]">
        {emptyMessage}
      </div>
    );
  }

  return (
    <div className={`overflow-x-auto ${className}`}>
      <table className="w-full">
        <thead className={stickyHeader ? 'sticky top-0 z-10' : ''}>
          <tr className="bg-[#141414] border-b border-[#2a2a2a]">
            {columns.map((column) => (
              <th
                key={column.key}
                className={`
                  px-4 py-3 text-left text-xs font-medium text-[#a0a0a0] uppercase tracking-wider
                  ${column.headerClassName || ''}
                `}
                style={{ width: column.width }}
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-[#2a2a2a]">
          {data.map((row, rowIndex) => (
            <tr
              key={row[rowKey] || rowIndex}
              onClick={() => onRowClick?.(row)}
              className={`
                bg-[#1a1a1a]
                ${onRowClick ? 'cursor-pointer hover:bg-[#1f1f1f]' : ''}
                transition-colors duration-150
              `}
            >
              {columns.map((column) => (
                <td
                  key={column.key}
                  className={`
                    px-4 py-4 text-sm text-white whitespace-nowrap
                    ${column.cellClassName || ''}
                  `}
                >
                  {column.render
                    ? column.render(row[column.key], row, rowIndex)
                    : row[column.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

// Compact variant for dense data
export const CompactTable = ({
  columns,
  data,
  loading = false,
  emptyMessage = 'No data available',
  className = '',
}) => {
  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Spinner size="sm" />
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="flex items-center justify-center py-8 text-sm text-[#a0a0a0]">
        {emptyMessage}
      </div>
    );
  }

  return (
    <div className={`overflow-x-auto ${className}`}>
      <table className="w-full">
        <thead>
          <tr className="border-b border-[#2a2a2a]">
            {columns.map((column) => (
              <th
                key={column.key}
                className={`
                  px-3 py-2 text-left text-xs font-medium text-[#666666] uppercase
                  ${column.headerClassName || ''}
                `}
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-[#1f1f1f]">
          {data.map((row, rowIndex) => (
            <tr key={row.id || rowIndex}>
              {columns.map((column) => (
                <td
                  key={column.key}
                  className={`
                    px-3 py-2 text-xs text-[#a0a0a0]
                    ${column.cellClassName || ''}
                  `}
                >
                  {column.render
                    ? column.render(row[column.key], row, rowIndex)
                    : row[column.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default Table;
