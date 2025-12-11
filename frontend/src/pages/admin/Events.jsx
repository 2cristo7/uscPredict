import { useState, useEffect } from 'react';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Input, { Textarea, Select } from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import Table from '../../components/common/Table';
import Spinner from '../../components/common/Spinner';

const EventForm = ({ event, onSave, onCancel, loading }) => {
  const [formData, setFormData] = useState({
    title: event?.title || '',
    description: event?.description || '',
    status: event?.status || 'DRAFT',
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <Input
        label="Title"
        value={formData.title}
        onChange={(e) => setFormData({ ...formData, title: e.target.value })}
        placeholder="Event title"
        required
      />
      <Textarea
        label="Description"
        value={formData.description}
        onChange={(e) => setFormData({ ...formData, description: e.target.value })}
        placeholder="Event description"
        rows={4}
      />
      <Select
        label="Status"
        value={formData.status}
        onChange={(e) => setFormData({ ...formData, status: e.target.value })}
        options={[
          { value: 'DRAFT', label: 'Draft' },
          { value: 'OPEN', label: 'Open' },
          { value: 'CLOSED', label: 'Closed' },
          { value: 'RESOLVED', label: 'Resolved' },
          { value: 'CANCELLED', label: 'Cancelled' },
        ]}
      />
      <div className="flex justify-end gap-3 pt-4">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={loading}>
          {event ? 'Update' : 'Create'} Event
        </Button>
      </div>
    </form>
  );
};

const Events = () => {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingEvent, setEditingEvent] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const fetchEvents = async () => {
    try {
      const response = await api.get('/events');
      setEvents(response.data);
    } catch (err) {
      console.error('Failed to fetch events:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
  }, []);

  const handleCreate = () => {
    setEditingEvent(null);
    setShowModal(true);
    setError('');
  };

  const handleEdit = (event) => {
    setEditingEvent(event);
    setShowModal(true);
    setError('');
  };

  const handleSave = async (formData) => {
    setSaving(true);
    setError('');
    try {
      if (editingEvent) {
        await api.patch(`/events/${editingEvent.uuid}`, [
          { op: 'replace', path: '/title', value: formData.title },
          { op: 'replace', path: '/description', value: formData.description },
          { op: 'replace', path: '/status', value: formData.status },
        ]);
      } else {
        await api.post('/events', formData);
      }
      setShowModal(false);
      fetchEvents();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save event');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (uuid) => {
    if (!confirm('Are you sure you want to delete this event?')) return;
    try {
      await api.delete(`/events/${uuid}`);
      fetchEvents();
    } catch (err) {
      console.error('Failed to delete event:', err);
    }
  };

  const columns = [
    {
      key: 'title',
      header: 'Title',
      render: (value) => <span className="font-medium">{value}</span>,
    },
    {
      key: 'description',
      header: 'Description',
      render: (value) => (
        <span className="text-[#a0a0a0] truncate max-w-xs block">
          {value || '-'}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (value) => <Badge status={value} />,
    },
    {
      key: 'markets',
      header: 'Markets',
      render: (value) => value?.length || 0,
    },
    {
      key: 'createdAt',
      header: 'Created',
      render: (value) => new Date(value).toLocaleDateString(),
    },
    {
      key: 'actions',
      header: '',
      render: (_, row) => (
        <div className="flex gap-2">
          <Button size="sm" variant="ghost" onClick={() => handleEdit(row)}>
            Edit
          </Button>
          <Button size="sm" variant="ghost" onClick={() => handleDelete(row.uuid)}>
            Delete
          </Button>
        </div>
      ),
    },
  ];

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold text-white">Events</h1>
        <Button onClick={handleCreate}>
          <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          Create Event
        </Button>
      </div>

      <Card padding="none">
        <Table
          columns={columns}
          data={events}
          emptyMessage="No events yet. Create your first event!"
        />
      </Card>

      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        title={editingEvent ? 'Edit Event' : 'Create Event'}
      >
        {error && (
          <div className="mb-4 p-3 bg-[#ef4444]/10 border border-[#ef4444]/20 rounded-lg">
            <p className="text-sm text-[#ef4444]">{error}</p>
          </div>
        )}
        <EventForm
          event={editingEvent}
          onSave={handleSave}
          onCancel={() => setShowModal(false)}
          loading={saving}
        />
      </Modal>
    </div>
  );
};

export default Events;
