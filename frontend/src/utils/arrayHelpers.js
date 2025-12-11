/**
 * Ensures the data is an array. Returns empty array if not.
 * @param {*} data - The data to check
 * @returns {Array} - The data as array or empty array
 */
export const ensureArray = (data) => {
  return Array.isArray(data) ? data : [];
};
