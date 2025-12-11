import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { eventAPI } from '../services/api';
import Badge from '../components/common/Badge';
import Spinner from '../components/common/Spinner';

// Stopwords to filter out when generating initials
const STOPWORDS = new Set([
  'el', 'la', 'los', 'las', 'de', 'del', 'en', 'a', 'y', 'o', 'un', 'una',
  'x', 'que', 'con', 'por', 'para', 'al', 'es', 'se', 'su', 'sus',
  'the', 'a', 'an', 'of', 'in', 'to', 'and', 'or', 'is', 'it', 'on', 'at', 'by'
]);

// Generate clean initials from title
const getInitials = (title) => {
  if (!title) return '?';

  // Remove special characters at start/end and split into words
  const cleanTitle = title
    .replace(/^[¿¡"'<>«»[\](){}.,;:!?@#$%^&*+=~`|\\/-]+/, '') // Remove leading special chars
    .replace(/[¿¡"'<>«»[\](){}.,;:!?@#$%^&*+=~`|\\/-]+$/, ''); // Remove trailing special chars

  // Split into words, filter stopwords and numbers-only words
  const words = cleanTitle
    .split(/\s+/)
    .map(word => word.replace(/^[^a-zA-ZáéíóúÁÉÍÓÚñÑ]+|[^a-zA-ZáéíóúÁÉÍÓÚñÑ]+$/g, '')) // Clean each word
    .filter(word => {
      if (!word) return false;
      if (STOPWORDS.has(word.toLowerCase())) return false;
      if (/^\d+$/.test(word)) return false; // Filter pure numbers
      if (word.length < 2) return false; // Filter single chars
      return true;
    });

  if (words.length === 0) {
    // Fallback: take first 2 letters of original title
    const fallback = title.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑ]/g, '');
    return (fallback.substring(0, 2) || '??').toUpperCase();
  }

  if (words.length === 1) {
    // Single significant word: take first 2 letters
    return words[0].substring(0, 2).toUpperCase();
  }

  // Multiple words: take first letter of first 2 significant words
  return (words[0][0] + words[1][0]).toUpperCase();
};

// Generate a deterministic color based on UUID
const getEventColor = (uuid) => {
  const colors = [
    'from-blue-600 to-purple-600',
    'from-green-600 to-teal-600',
    'from-orange-600 to-red-600',
    'from-pink-600 to-rose-600',
    'from-indigo-600 to-blue-600',
    'from-amber-600 to-orange-600',
    'from-cyan-600 to-blue-600',
    'from-violet-600 to-purple-600',
  ];
  const hash = uuid?.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) || 0;
  return colors[hash % colors.length];
};

// Format volume with K/M suffixes
const formatVolume = (volume) => {
  if (!volume || volume === 0) return '$0';
  if (volume >= 1000000) return `$${(volume / 1000000).toFixed(1)}M`;
  if (volume >= 1000) return `$${(volume / 1000).toFixed(1)}K`;
  return `$${volume.toFixed(0)}`;
};

// Format date for display
const formatDate = (dateStr) => {
  if (!dateStr) return null;
  const date = new Date(dateStr);
  return date.toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
};

// Placeholder image component with initials
const EventPlaceholder = ({ title, uuid }) => {
  const gradientClass = getEventColor(uuid);
  const initials = getInitials(title);

  return (
    <div className={`w-full h-36 bg-gradient-to-br ${gradientClass} flex items-center justify-center relative`}>
      <span className="text-5xl font-bold text-white/25">{initials}</span>
    </div>
  );
};

// Market Card Component
const MarketCard = ({ event }) => {
  const market = event.markets?.[0];

  // Calculate probability from lastPrice, bestBid, or bestAsk
  let yesProb = 50; // Default
  if (market?.lastPrice && market.lastPrice > 0) {
    yesProb = Math.round(market.lastPrice * 100);
  } else if (market?.bestBid || market?.bestAsk) {
    // Use midpoint of best bid/ask if available
    const bid = market.bestBid || 0;
    const ask = market.bestAsk || 1;
    yesProb = Math.round(((bid + ask) / 2) * 100);
  }
  const noProb = 100 - yesProb;

  return (
    <Link to={`/event/${event.uuid}`}>
      <div className="h-full bg-[#1a1a1a] border border-[#333333] rounded-xl overflow-hidden hover:border-[#444444] hover:bg-[#1e1e1e] hover:shadow-lg hover:shadow-black/20 transition-all duration-200 hover:-translate-y-0.5">
        {/* Placeholder Image */}
        <EventPlaceholder title={event.title} uuid={event.uuid} />

        <div className="p-5">
          {/* Title and Badge */}
          <div className="flex items-start gap-3 mb-3">
            <h3 className="text-white font-medium leading-tight line-clamp-2 flex-1 text-sm">
              {event.title}
            </h3>
          </div>

          {/* Status and Resolution Date */}
          <div className="flex items-center gap-2 mb-4 text-xs">
            <Badge status={event.status} size="sm" />
            {event.resolutionDate && (
              <>
                <span className="text-[#555555]">·</span>
                <span className="text-[#888888]">Resuelve: {formatDate(event.resolutionDate)}</span>
              </>
            )}
          </div>

          {/* Probability bars */}
          <div className="space-y-2.5">
            {/* YES Bar */}
            <div className="flex items-center gap-3">
              <span className="text-xs text-[#22c55e] font-semibold w-8">YES</span>
              <div className="flex-1 h-2.5 bg-[#1a1a1a] border border-[#333333] rounded-full overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-[#22c55e] to-[#16a34a] rounded-full transition-all duration-500"
                  style={{ width: `${yesProb}%` }}
                />
              </div>
              <span className="text-sm text-[#22c55e] font-bold w-12 text-right">
                {yesProb}%
              </span>
            </div>

            {/* NO Bar */}
            <div className="flex items-center gap-3">
              <span className="text-xs text-[#ef4444] font-semibold w-8">NO</span>
              <div className="flex-1 h-2.5 bg-[#1a1a1a] border border-[#333333] rounded-full overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-[#ef4444] to-[#dc2626] rounded-full transition-all duration-500"
                  style={{ width: `${noProb}%` }}
                />
              </div>
              <span className="text-sm text-[#ef4444] font-bold w-12 text-right">
                {noProb}%
              </span>
            </div>
          </div>

          {/* Footer stats */}
          <div className="flex items-center justify-between mt-4 pt-4 border-t border-[#2a2a2a]">
            <span className="text-xs text-[#888888] font-medium">
              Vol: {formatVolume(market?.volume)}
            </span>
            <span className="text-xs text-[#666666]">
              {event.markets?.length || 0} mercado{event.markets?.length !== 1 ? 's' : ''}
            </span>
          </div>
        </div>
      </div>
    </Link>
  );
};

// Export utilities for use in EventDetail
// eslint-disable-next-line react-refresh/only-export-components
export { EventPlaceholder, getEventColor, getInitials };

const Home = () => {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const fetchEvents = async () => {
      try {
        const response = await eventAPI.v1.getAll();
        // Ensure data is an array
        const eventsData = Array.isArray(response.data) ? response.data : [];
        setEvents(eventsData);
      } catch (err) {
        setError('Failed to load events');
        console.error(err);
        setEvents([]); // Set empty array on error
      } finally {
        setLoading(false);
      }
    };
    fetchEvents();
  }, []);

  // Filter events by search query
  const filteredEvents = events.filter((event) =>
    event.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    event.description?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Separate active and other events
  const activeEvents = filteredEvents.filter((e) => e.status === 'OPEN');
  const otherEvents = filteredEvents.filter((e) => e.status !== 'OPEN');

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Hero Section */}
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold text-white mb-4">
          Predict the Future
        </h1>
        <p className="text-lg text-[#a0a0a0] max-w-2xl mx-auto">
          Trade on the outcome of real-world events. Buy YES or NO shares and profit from your predictions.
        </p>
      </div>

      {/* Search Bar */}
      <div className="max-w-xl mx-auto mb-10">
        <div className="relative">
          <svg
            className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-[#666666]"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
            />
          </svg>
          <input
            type="text"
            placeholder="Search markets..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-12 pr-4 py-3.5 bg-[#1a1a1a] border border-[#333333] rounded-xl text-white placeholder-[#666666] focus:outline-none focus:border-[#3b82f6] focus:ring-1 focus:ring-[#3b82f6] transition-all"
          />
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : error ? (
        <div className="text-center py-12">
          <p className="text-[#ef4444]">{error}</p>
        </div>
      ) : (
        <>
          {/* Active Markets */}
          {activeEvents.length > 0 && (
            <section className="mb-12">
              <h2 className="text-xl font-semibold text-white mb-6 flex items-center gap-2">
                <span className="w-2 h-2 bg-[#22c55e] rounded-full animate-pulse" />
                Active Markets
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {activeEvents.map((event) => (
                  <MarketCard key={event.uuid} event={event} />
                ))}
              </div>
            </section>
          )}

          {/* Other Markets */}
          {otherEvents.length > 0 && (
            <section>
              <h2 className="text-xl font-semibold text-white mb-6">
                All Markets
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {otherEvents.map((event) => (
                  <MarketCard key={event.uuid} event={event} />
                ))}
              </div>
            </section>
          )}

          {/* Empty state */}
          {filteredEvents.length === 0 && (
            <div className="text-center py-12">
              <p className="text-[#a0a0a0]">
                {searchQuery ? 'No markets found matching your search.' : 'No markets available yet.'}
              </p>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default Home;
