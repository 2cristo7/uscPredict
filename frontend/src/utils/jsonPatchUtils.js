/**
 * Utility functions for creating JSON-Patch operations (RFC 6902)
 * These helpers make it easier to construct JSON-Patch requests
 */

/**
 * Creates a "replace" operation for JSON-Patch
 * Used to replace an existing value at a specific path
 * @param {string} path - The JSON pointer path (e.g., "/state", "/title")
 * @param {*} value - The new value to set
 * @returns {Object} JSON-Patch operation object
 */
export const replaceOp = (path, value) => ({
  op: 'replace',
  path,
  value,
});

/**
 * Creates an "add" operation for JSON-Patch
 * Used to add a new value or append to an array
 * @param {string} path - The JSON pointer path
 * @param {*} value - The value to add
 * @returns {Object} JSON-Patch operation object
 */
export const addOp = (path, value) => ({
  op: 'add',
  path,
  value,
});

/**
 * Creates a "remove" operation for JSON-Patch
 * Used to remove a value at a specific path
 * @param {string} path - The JSON pointer path
 * @returns {Object} JSON-Patch operation object
 */
export const removeOp = (path) => ({
  op: 'remove',
  path,
});

/**
 * Creates a "copy" operation for JSON-Patch
 * Used to copy a value from one location to another
 * @param {string} from - The source path
 * @param {string} path - The destination path
 * @returns {Object} JSON-Patch operation object
 */
export const copyOp = (from, path) => ({
  op: 'copy',
  from,
  path,
});

/**
 * Creates a "move" operation for JSON-Patch
 * Used to move a value from one location to another
 * @param {string} from - The source path
 * @param {string} path - The destination path
 * @returns {Object} JSON-Patch operation object
 */
export const moveOp = (from, path) => ({
  op: 'move',
  from,
  path,
});

/**
 * Creates a "test" operation for JSON-Patch
 * Used to test that a value at a path equals a specific value
 * @param {string} path - The JSON pointer path
 * @param {*} value - The expected value
 * @returns {Object} JSON-Patch operation object
 */
export const testOp = (path, value) => ({
  op: 'test',
  path,
  value,
});

/**
 * Helper to create multiple replace operations at once
 * @param {Object} updates - Object with path-value pairs
 * @returns {Array} Array of JSON-Patch operations
 * @example
 * multipleReplace({ "/title": "New Title", "/state": "CLOSED" })
 */
export const multipleReplace = (updates) => {
  return Object.entries(updates).map(([path, value]) => replaceOp(path, value));
};
