import { useState, useEffect } from 'react';
import { eventAPI, commentAPI, userAPI } from '../services/api';

export default function Events() {
  const [events, setEvents] = useState([]);
  const [users, setUsers] = useState([]);
  const [expandedEventId, setExpandedEventId] = useState(null);
  const [comments, setComments] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [newComment, setNewComment] = useState({
    content: '',
    userId: '',
  });
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editContent, setEditContent] = useState('');

  useEffect(() => {
    loadEvents();
    loadUsers();
  }, []);

  const loadEvents = async () => {
    try {
      setLoading(true);
      const response = await eventAPI.getAll();
      setEvents(response.data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const loadUsers = async () => {
    try {
      const response = await userAPI.getAll();
      setUsers(response.data);
    } catch (err) {
      console.error('Error loading users:', err);
    }
  };

  const loadComments = async (eventId) => {
    try {
      const response = await commentAPI.getByPostId(eventId);
      setComments(prev => ({
        ...prev,
        [eventId]: response.data
      }));
    } catch (err) {
      console.error('Error loading comments:', err);
    }
  };

  const handleEventClick = async (eventId) => {
    if (expandedEventId === eventId) {
      setExpandedEventId(null);
    } else {
      setExpandedEventId(eventId);
      if (!comments[eventId]) {
        await loadComments(eventId);
      }
    }
  };

  const handleCreateComment = async (e, eventId) => {
    e.preventDefault();
    if (!newComment.userId || !newComment.content.trim()) {
      alert('Please select a user and enter a comment');
      return;
    }

    try {
      await commentAPI.create({
        content: newComment.content,
        userId: newComment.userId,
        postId: eventId
      });
      setNewComment({ content: '', userId: '' });
      await loadComments(eventId);
    } catch (err) {
      setError(err.message);
    }
  };

  const getUserName = (userId) => {
    const user = users.find(u => u.uuid === userId);
    return user ? user.name : 'Unknown User';
  };

  const handleChangeEventState = async (eventId, newState) => {
    try {
      setError(null);
      await eventAPI.changeState(eventId, newState);
      await loadEvents();
    } catch (err) {
      setError(err.message);
    }
  };

  const handleEditComment = (comment) => {
    setEditingCommentId(comment.uuid);
    setEditContent(comment.content);
  };

  const handleSaveEdit = async (eventId) => {
    if (!editContent.trim()) {
      alert('Comment cannot be empty');
      return;
    }

    try {
      setError(null);
      await commentAPI.editContent(editingCommentId, editContent);
      setEditingCommentId(null);
      setEditContent('');
      await loadComments(eventId);
    } catch (err) {
      setError(err.message);
    }
  };

  const handleCancelEdit = () => {
    setEditingCommentId(null);
    setEditContent('');
  };

  const getStateColor = (state) => {
    switch (state) {
      case 'OPEN':
        return 'bg-green-100 text-green-800';
      case 'CLOSED':
        return 'bg-yellow-100 text-yellow-800';
      case 'SETTLED':
        return 'bg-blue-100 text-blue-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) return <div className="text-center py-8">Loading...</div>;

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold">Eventos</h2>
        <button
          onClick={loadEvents}
          className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
        >
          Refresh
        </button>
      </div>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          Error: {error}
        </div>
      )}

      <div className="space-y-4">
        {events.length === 0 ? (
          <div className="bg-white shadow-md rounded px-8 py-6 text-center text-gray-500">
            No events found
          </div>
        ) : (
          events.map((event) => (
            <div key={event.uuid} className="bg-white shadow-md rounded overflow-hidden">
              {/* Event Header - Clickable */}
              <div
                onClick={() => handleEventClick(event.uuid)}
                className="cursor-pointer hover:bg-gray-50 transition-colors p-6"
              >
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <h3 className="text-xl font-semibold text-gray-900 mb-2">
                      {event.title}
                    </h3>
                    <p className="text-sm text-gray-600 line-clamp-2">
                      {event.description}
                    </p>
                  </div>
                  <div className="flex items-center space-x-4 ml-4">
                    <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStateColor(event.state)}`}>
                      {event.state}
                    </span>
                    <svg
                      className={`w-5 h-5 text-gray-500 transition-transform ${
                        expandedEventId === event.uuid ? 'transform rotate-180' : ''
                      }`}
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </div>
                </div>
                <div className="mt-2 text-xs text-gray-500">
                  Created: {new Date(event.createdAt).toLocaleString()}
                </div>
              </div>

              {/* Expanded Content - Event Details and Comments */}
              {expandedEventId === event.uuid && (
                <div className="border-t border-gray-200 bg-gray-50 p-6">
                  {/* Event Full Details */}
                  <div className="mb-6 bg-white p-4 rounded">
                    <h4 className="font-semibold text-gray-700 mb-2">Event Details</h4>
                    <div className="space-y-2 text-sm">
                      <p><span className="font-medium">ID:</span> {event.uuid}</p>
                      <p><span className="font-medium">Title:</span> {event.title}</p>
                      <p><span className="font-medium">Description:</span> {event.description || 'No description'}</p>
                      <p><span className="font-medium">State:</span> <span className={`px-2 py-1 rounded text-xs ${getStateColor(event.state)}`}>{event.state}</span></p>
                      <p><span className="font-medium">Created:</span> {new Date(event.createdAt).toLocaleString()}</p>
                    </div>
                    {/* State Change Actions */}
                    <div className="mt-4 pt-4 border-t border-gray-200">
                      <p className="text-xs font-medium text-gray-600 mb-2">Change Event State:</p>
                      <div className="flex space-x-2">
                        {event.state !== 'OPEN' && (
                          <button
                            onClick={() => handleChangeEventState(event.uuid, 'OPEN')}
                            className="text-xs bg-green-500 hover:bg-green-700 text-white font-bold py-1 px-3 rounded"
                          >
                            Set OPEN
                          </button>
                        )}
                        {event.state !== 'CLOSED' && (
                          <button
                            onClick={() => handleChangeEventState(event.uuid, 'CLOSED')}
                            className="text-xs bg-yellow-500 hover:bg-yellow-700 text-white font-bold py-1 px-3 rounded"
                          >
                            Set CLOSED
                          </button>
                        )}
                        {event.state !== 'SETTLED' && (
                          <button
                            onClick={() => handleChangeEventState(event.uuid, 'SETTLED')}
                            className="text-xs bg-blue-500 hover:bg-blue-700 text-white font-bold py-1 px-3 rounded"
                          >
                            Set SETTLED
                          </button>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Comments Section */}
                  <div className="space-y-4">
                    <h4 className="font-semibold text-gray-700">Comments</h4>

                    {/* Comment Form */}
                    <form onSubmit={(e) => handleCreateComment(e, event.uuid)} className="bg-white p-4 rounded shadow">
                      <div className="mb-3">
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Select User
                        </label>
                        <select
                          value={newComment.userId}
                          onChange={(e) => setNewComment({ ...newComment, userId: e.target.value })}
                          className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                          required
                        >
                          <option value="">-- Select a user --</option>
                          {users.map((user) => (
                            <option key={user.uuid} value={user.uuid}>
                              {user.name} ({user.email})
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className="mb-3">
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Comment
                        </label>
                        <textarea
                          value={newComment.content}
                          onChange={(e) => setNewComment({ ...newComment, content: e.target.value })}
                          className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                          rows="3"
                          placeholder="Write your comment here..."
                          required
                        />
                      </div>
                      <button
                        type="submit"
                        className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded text-sm"
                      >
                        Post Comment
                      </button>
                    </form>

                    {/* Comments List */}
                    <div className="space-y-3">
                      {!comments[event.uuid] ? (
                        <div className="text-center text-gray-500 py-4">Loading comments...</div>
                      ) : comments[event.uuid].length === 0 ? (
                        <div className="text-center text-gray-500 py-4 bg-white rounded">
                          No comments yet. Be the first to comment!
                        </div>
                      ) : (
                        comments[event.uuid].map((comment) => (
                          <div key={comment.uuid} className="bg-white p-4 rounded shadow">
                            <div className="flex justify-between items-start mb-2">
                              <div className="font-medium text-gray-900">
                                {getUserName(comment.userId)}
                              </div>
                              <div className="flex items-center space-x-2">
                                <div className="text-xs text-gray-500">
                                  {new Date(comment.createdAt).toLocaleString()}
                                </div>
                                {editingCommentId !== comment.uuid && (
                                  <button
                                    onClick={() => handleEditComment(comment)}
                                    className="text-xs text-blue-600 hover:text-blue-800"
                                  >
                                    Edit
                                  </button>
                                )}
                              </div>
                            </div>
                            {editingCommentId === comment.uuid ? (
                              <div className="space-y-2">
                                <textarea
                                  value={editContent}
                                  onChange={(e) => setEditContent(e.target.value)}
                                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                  rows="3"
                                />
                                <div className="flex space-x-2">
                                  <button
                                    onClick={() => handleSaveEdit(event.uuid)}
                                    className="bg-green-500 hover:bg-green-700 text-white text-xs font-bold py-1 px-3 rounded"
                                  >
                                    Save
                                  </button>
                                  <button
                                    onClick={handleCancelEdit}
                                    className="bg-gray-500 hover:bg-gray-700 text-white text-xs font-bold py-1 px-3 rounded"
                                  >
                                    Cancel
                                  </button>
                                </div>
                              </div>
                            ) : (
                              <p className="text-sm text-gray-700">{comment.content}</p>
                            )}
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
