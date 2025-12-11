import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { eventAPI, orderAPI, positionAPI, commentAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { ensureArray } from '../utils/arrayHelpers';
import Card from '../components/common/Card';
import Badge from '../components/common/Badge';
import Button from '../components/common/Button';
import Input, { Textarea } from '../components/common/Input';
import Spinner from '../components/common/Spinner';
import { getEventColor, getInitials } from './Home';

// ============================================================================
// PRICE CHART PLACEHOLDER
// ============================================================================
const PriceChart = ({ yesProb }) => {
  return (
    <Card className="relative overflow-hidden">
      {/* Mock chart background */}
      <div className="h-48 relative">
        {/* Y-axis labels */}
        <div className="absolute left-0 top-0 bottom-0 w-8 flex flex-col justify-between text-xs text-[#666666] py-2">
          <span>100%</span>
          <span>75%</span>
          <span>50%</span>
          <span>25%</span>
          <span>0%</span>
        </div>

        {/* Chart area */}
        <div className="ml-10 h-full relative border-l border-b border-[#2a2a2a]">
          {/* Grid lines */}
          <div className="absolute inset-0 flex flex-col justify-between pointer-events-none">
            {[0, 1, 2, 3, 4].map((i) => (
              <div key={i} className="border-t border-[#1a1a1a] w-full" />
            ))}
          </div>

          {/* Current price line */}
          <div
            className="absolute left-0 right-0 border-t-2 border-dashed border-[#22c55e]/50"
            style={{ top: `${100 - yesProb}%` }}
          >
            <span className="absolute right-0 -top-3 bg-[#22c55e] text-white text-xs px-2 py-0.5 rounded">
              {yesProb}%
            </span>
          </div>

          {/* Placeholder message */}
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-center">
              <svg className="w-8 h-8 mx-auto text-[#333333] mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 12l3-3 3 3 4-4M8 21l4-4 4 4M3 4h18M4 4h16v12a1 1 0 01-1 1H5a1 1 0 01-1-1V4z" />
              </svg>
              <p className="text-[#555555] text-sm">Price history coming soon</p>
            </div>
          </div>
        </div>

        {/* X-axis labels */}
        <div className="ml-10 flex justify-between text-xs text-[#666666] mt-2">
          <span>1D</span>
          <span>1W</span>
          <span>1M</span>
          <span>ALL</span>
        </div>
      </div>

      {/* Current price display */}
      <div className="mt-4 pt-4 border-t border-[#2a2a2a] flex items-center justify-between">
        <div>
          <p className="text-xs text-[#666666] uppercase">Current Price</p>
          <p className="text-2xl font-bold text-[#22c55e]">{yesProb}c</p>
        </div>
        <div className="text-right">
          <p className="text-xs text-[#666666] uppercase">Implied Probability</p>
          <p className="text-2xl font-bold text-white">{yesProb}%</p>
        </div>
      </div>
    </Card>
  );
};

// ============================================================================
// ORDER BOOK - Aggregated by price level
// ============================================================================
const OrderBook = ({ market, refreshKey }) => {
  const [orders, setOrders] = useState({ bids: [], asks: [] });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOrders = async () => {
      if (!market?.uuid) {
        setLoading(false);
        return;
      }
      try {
        const response = await orderAPI.v1.getByMarketId(market.uuid);
        const allOrders = response.data;

        // Filter pending orders and aggregate by price level
        const pendingOrders = allOrders.filter(o => o.status === 'PENDING');

        // Aggregate bids by price
        const bidMap = new Map();
        pendingOrders
          .filter(o => o.side === 'BUY')
          .forEach(o => {
            const priceKey = o.price.toFixed(2);
            const existing = bidMap.get(priceKey) || { price: o.price, quantity: 0, orders: 0 };
            existing.quantity += o.quantity - (o.filledQuantity || 0);
            existing.orders += 1;
            bidMap.set(priceKey, existing);
          });

        // Aggregate asks by price
        const askMap = new Map();
        pendingOrders
          .filter(o => o.side === 'SELL')
          .forEach(o => {
            const priceKey = o.price.toFixed(2);
            const existing = askMap.get(priceKey) || { price: o.price, quantity: 0, orders: 0 };
            existing.quantity += o.quantity - (o.filledQuantity || 0);
            existing.orders += 1;
            askMap.set(priceKey, existing);
          });

        const bids = Array.from(bidMap.values())
          .sort((a, b) => b.price - a.price)
          .slice(0, 8);
        const asks = Array.from(askMap.values())
          .sort((a, b) => a.price - b.price)
          .slice(0, 8);

        setOrders({ bids, asks });
      } catch (err) {
        console.error('Failed to fetch orders:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchOrders();
    const interval = setInterval(fetchOrders, 5000);
    return () => clearInterval(interval);
  }, [market?.uuid, refreshKey]);

  const maxQuantity = Math.max(
    ...orders.bids.map(o => o.quantity),
    ...orders.asks.map(o => o.quantity),
    1
  );

  const bestBid = orders.bids[0]?.price;
  const bestAsk = orders.asks[0]?.price;
  const spread = bestBid && bestAsk ? ((bestAsk - bestBid) * 100).toFixed(1) : null;

  if (loading) {
    return (
      <Card>
        <div className="flex justify-center py-8"><Spinner /></div>
      </Card>
    );
  }

  // Show message if no market
  if (!market) {
    return (
      <Card>
        <h3 className="text-lg font-semibold text-white mb-4">Order Book</h3>
        <div className="text-center py-8">
          <p className="text-[#555555]">No market available yet</p>
        </div>
      </Card>
    );
  }

  return (
    <Card>
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-white">Order Book</h3>
        {spread && (
          <span className="text-xs text-[#666666] bg-[#1a1a1a] px-2 py-1 rounded">
            Spread: {spread}c
          </span>
        )}
      </div>

      <div className="grid grid-cols-2 gap-6">
        {/* Bids (Buy orders) */}
        <div>
          <div className="text-xs text-[#666666] uppercase mb-3 flex justify-between px-2 font-medium">
            <span>BIDS (BUY)</span>
            <span>QTY</span>
          </div>
          <div className="space-y-1">
            {orders.bids.length === 0 ? (
              <p className="text-xs text-[#555555] text-center py-6">No buy orders</p>
            ) : (
              orders.bids.map((level, i) => (
                <div key={i} className="relative flex justify-between px-2 py-1.5 text-sm rounded">
                  <div
                    className="absolute inset-0 bg-[#22c55e]/15 rounded"
                    style={{ width: `${(level.quantity / maxQuantity) * 100}%` }}
                  />
                  <span className="relative text-[#22c55e] font-medium">
                    {(level.price * 100).toFixed(0)}c
                  </span>
                  <span className="relative text-white font-medium">{level.quantity}</span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Asks (Sell orders) */}
        <div>
          <div className="text-xs text-[#666666] uppercase mb-3 flex justify-between px-2 font-medium">
            <span>ASKS (SELL)</span>
            <span>QTY</span>
          </div>
          <div className="space-y-1">
            {orders.asks.length === 0 ? (
              <p className="text-xs text-[#555555] text-center py-6">No sell orders</p>
            ) : (
              orders.asks.map((level, i) => (
                <div key={i} className="relative flex justify-between px-2 py-1.5 text-sm rounded">
                  <div
                    className="absolute inset-0 right-0 bg-[#ef4444]/15 rounded"
                    style={{ width: `${(level.quantity / maxQuantity) * 100}%`, marginLeft: 'auto' }}
                  />
                  <span className="relative text-[#ef4444] font-medium">
                    {(level.price * 100).toFixed(0)}c
                  </span>
                  <span className="relative text-white font-medium">{level.quantity}</span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </Card>
  );
};

// ============================================================================
// TRADING PANEL - Full BUY/SELL functionality
// ============================================================================
const TradingPanel = ({ market, event, onOrderPlaced }) => {
  const { isAuthenticated } = useAuth();
  const [shareType, setShareType] = useState('YES');
  const [orderType, setOrderType] = useState('BUY');
  const [quantity, setQuantity] = useState('');
  const [price, setPrice] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const priceNum = parseFloat(price) || 0;
  const quantityNum = parseInt(quantity) || 0;

  // Calculate costs and potential returns
  const cost = orderType === 'BUY' ? priceNum * quantityNum : 0;
  const potentialPayout = quantityNum; // Each share pays $1 if correct
  const potentialProfit = potentialPayout - cost;
  const returnPct = cost > 0 ? ((potentialProfit / cost) * 100).toFixed(0) : 0;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isAuthenticated || !market) return;

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      await orderAPI.v1.create({
        marketUuid: market.uuid,
        side: orderType,
        shareType: shareType,
        quantity: quantityNum,
        price: priceNum,
      });
      setSuccess(`Order placed: ${orderType} ${quantityNum} ${shareType} @ ${(priceNum * 100).toFixed(0)}c`);
      setQuantity('');
      setPrice('');
      onOrderPlaced?.();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data || 'Failed to place order');
    } finally {
      setLoading(false);
    }
  };

  const hasMarket = !!market;
  const canTrade = hasMarket && event?.status === 'OPEN' && market?.status === 'ACTIVE';
  const isYes = shareType === 'YES';

  // Show message if no market exists
  if (!hasMarket) {
    return (
      <Card>
        <h3 className="text-lg font-semibold text-white mb-4">Trade</h3>
        <div className="text-center py-8">
          <svg className="w-12 h-12 mx-auto text-[#333333] mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p className="text-[#888888] mb-2">No market available</p>
          <p className="text-[#555555] text-sm">A market needs to be created for this event before trading can begin.</p>
        </div>
      </Card>
    );
  }

  return (
    <Card>
      <h3 className="text-lg font-semibold text-white mb-4">Trade</h3>

      {/* Share Type Selection */}
      <div className="grid grid-cols-2 gap-2 mb-4">
        <button
          onClick={() => setShareType('YES')}
          className={`py-3 px-4 rounded-lg font-semibold transition-all ${
            shareType === 'YES'
              ? 'bg-[#22c55e] text-white'
              : 'bg-[#1a1a1a] text-[#666666] hover:text-white border border-[#2a2a2a]'
          }`}
        >
          YES
          <span className="block text-xs font-normal opacity-75">
            {market?.lastPrice ? `${Math.round(market.lastPrice * 100)}c` : '50c'}
          </span>
        </button>
        <button
          onClick={() => setShareType('NO')}
          className={`py-3 px-4 rounded-lg font-semibold transition-all ${
            shareType === 'NO'
              ? 'bg-[#ef4444] text-white'
              : 'bg-[#1a1a1a] text-[#666666] hover:text-white border border-[#2a2a2a]'
          }`}
        >
          NO
          <span className="block text-xs font-normal opacity-75">
            {market?.lastPrice ? `${Math.round((1 - market.lastPrice) * 100)}c` : '50c'}
          </span>
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Feedback messages */}
        {error && (
          <div className="p-3 bg-[#ef4444]/10 border border-[#ef4444]/30 rounded-lg">
            <p className="text-sm text-[#ef4444]">{error}</p>
          </div>
        )}
        {success && (
          <div className="p-3 bg-[#22c55e]/10 border border-[#22c55e]/30 rounded-lg">
            <p className="text-sm text-[#22c55e]">{success}</p>
          </div>
        )}

        {/* Quantity */}
        <div>
          <label className="block text-sm font-medium text-[#a0a0a0] mb-2">
            Shares
          </label>
          <input
            type="number"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            placeholder="0"
            min="1"
            className="w-full px-4 py-3 bg-[#0a0a0a] border border-[#2a2a2a] rounded-lg text-white text-lg font-medium placeholder-[#555555] focus:outline-none focus:border-[#3b82f6]"
          />
        </div>

        {/* Price */}
        <div>
          <label className="block text-sm font-medium text-[#a0a0a0] mb-2">
            Limit Price (1-99c)
          </label>
          <div className="relative">
            <input
              type="number"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              placeholder="0.50"
              min="0.01"
              max="0.99"
              step="0.01"
              className="w-full px-4 py-3 bg-[#0a0a0a] border border-[#2a2a2a] rounded-lg text-white text-lg font-medium placeholder-[#555555] focus:outline-none focus:border-[#3b82f6]"
            />
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-[#666666]">
              = {priceNum ? (priceNum * 100).toFixed(0) : 0}c
            </span>
          </div>
        </div>

        {/* Order Summary */}
        {quantityNum > 0 && priceNum > 0 && (
          <div className="p-4 bg-[#0a0a0a] rounded-lg border border-[#2a2a2a] space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-[#888888]">Cost</span>
              <span className="text-white font-medium">${cost.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-[#888888]">Payout if {shareType} wins</span>
              <span className="text-white font-medium">${potentialPayout.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-sm pt-2 border-t border-[#2a2a2a]">
              <span className="text-[#888888]">Potential Profit</span>
              <span className={`font-bold ${isYes ? 'text-[#22c55e]' : 'text-[#ef4444]'}`}>
                +${potentialProfit.toFixed(2)} ({returnPct}%)
              </span>
            </div>
          </div>
        )}

        {/* Action Buttons */}
        {!isAuthenticated ? (
          <Link to="/login" className="block">
            <Button fullWidth variant="secondary" size="lg">
              Sign in to trade
            </Button>
          </Link>
        ) : !canTrade ? (
          <Button fullWidth disabled size="lg">
            Market not active
          </Button>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            <Button
              type="submit"
              onClick={() => setOrderType('BUY')}
              fullWidth
              loading={loading && orderType === 'BUY'}
              variant="success"
              size="lg"
              disabled={!quantityNum || !priceNum}
            >
              BUY
            </Button>
            <Button
              type="submit"
              onClick={() => setOrderType('SELL')}
              fullWidth
              loading={loading && orderType === 'SELL'}
              variant="danger"
              size="lg"
              disabled={!quantityNum || !priceNum}
            >
              SELL
            </Button>
          </div>
        )}
      </form>
    </Card>
  );
};

// ============================================================================
// USER POSITION
// ============================================================================
const UserPosition = ({ market, user, refreshKey }) => {
  const [position, setPosition] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPosition = async () => {
      if (!market?.uuid || !user?.uuid) {
        setLoading(false);
        return;
      }
      try {
        const response = await positionAPI.v1.getByUserId(user.uuid);
        const positions = response.data;
        const marketPosition = positions.find(p => p.market?.uuid === market.uuid);
        setPosition(marketPosition);
      } catch (err) {
        console.error('Failed to fetch position:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchPosition();
  }, [market?.uuid, user?.uuid, refreshKey]);

  if (loading) return null;
  if (!position) return null;

  const hasPosition = (position.yesShares > 0) || (position.noShares > 0);
  if (!hasPosition) return null;

  const currentPrice = market?.lastPrice || 0.5;
  const yesValue = position.yesShares * currentPrice;
  const noValue = position.noShares * (1 - currentPrice);
  const totalValue = yesValue + noValue;
  const avgCost = position.averageCost || 0;
  const pnl = totalValue - avgCost;
  const pnlPct = avgCost > 0 ? ((pnl / avgCost) * 100).toFixed(1) : 0;

  return (
    <Card>
      <h3 className="text-lg font-semibold text-white mb-4">Your Position</h3>
      <div className="grid grid-cols-2 gap-4">
        <div className="p-3 bg-[#22c55e]/10 rounded-lg border border-[#22c55e]/20">
          <p className="text-xs text-[#22c55e] uppercase font-medium">YES Shares</p>
          <p className="text-2xl font-bold text-white">{position.yesShares || 0}</p>
        </div>
        <div className="p-3 bg-[#ef4444]/10 rounded-lg border border-[#ef4444]/20">
          <p className="text-xs text-[#ef4444] uppercase font-medium">NO Shares</p>
          <p className="text-2xl font-bold text-white">{position.noShares || 0}</p>
        </div>
      </div>
      <div className="mt-4 pt-4 border-t border-[#2a2a2a] flex justify-between items-center">
        <div>
          <p className="text-xs text-[#666666]">Current Value</p>
          <p className="text-lg font-semibold text-white">${totalValue.toFixed(2)}</p>
        </div>
        <div className="text-right">
          <p className="text-xs text-[#666666]">P&L</p>
          <p className={`text-lg font-semibold ${pnl >= 0 ? 'text-[#22c55e]' : 'text-[#ef4444]'}`}>
            {pnl >= 0 ? '+' : ''}{pnl.toFixed(2)} ({pnlPct}%)
          </p>
        </div>
      </div>
    </Card>
  );
};

// ============================================================================
// MARKET STATS
// ============================================================================
const MarketStats = ({ event, market }) => {
  const formatDate = (date) => {
    if (!date) return 'TBD';
    return new Date(date).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const formatVolume = (vol) => {
    if (!vol) return '$0';
    if (vol >= 1000000) return `$${(vol / 1000000).toFixed(1)}M`;
    if (vol >= 1000) return `$${(vol / 1000).toFixed(1)}K`;
    return `$${vol.toFixed(0)}`;
  };

  return (
    <Card>
      <h3 className="text-lg font-semibold text-white mb-4">Market Info</h3>
      <div className="space-y-3">
        <div className="flex justify-between">
          <span className="text-[#888888]">Volume</span>
          <span className="text-white font-medium">{formatVolume(market?.volume)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-[#888888]">Created</span>
          <span className="text-white">{formatDate(event?.createdAt)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-[#888888]">Resolves</span>
          <span className="text-white">{formatDate(event?.resolutionDate)}</span>
        </div>
        {event?.resolutionDetails && (
          <div className="pt-3 border-t border-[#2a2a2a]">
            <p className="text-xs text-[#666666] uppercase mb-1">Resolution Criteria</p>
            <p className="text-sm text-[#a0a0a0]">{event.resolutionDetails}</p>
          </div>
        )}
      </div>
    </Card>
  );
};

// ============================================================================
// COMMENTS SECTION
// ============================================================================
const CommentsSection = ({ eventId }) => {
  const { isAuthenticated } = useAuth();
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const fetchComments = useCallback(async () => {
    try {
      const response = await commentAPI.v1.getByEventId(eventId);
      setComments(ensureArray(response.data));
    } catch (err) {
      console.error('Failed to fetch comments:', err);
      setComments([]);
    } finally {
      setLoading(false);
    }
  }, [eventId]);

  useEffect(() => {
    fetchComments();
  }, [fetchComments]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!newComment.trim() || !isAuthenticated) return;

    setSubmitting(true);
    try {
      await commentAPI.v1.create({
        eventUuid: eventId,
        content: newComment.trim(),
      });
      setNewComment('');
      fetchComments();
    } catch (err) {
      console.error('Failed to post comment:', err);
    } finally {
      setSubmitting(false);
    }
  };

  const formatTimeAgo = (date) => {
    const now = new Date();
    const diff = now - new Date(date);
    const mins = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    return new Date(date).toLocaleDateString();
  };

  return (
    <Card>
      <h3 className="text-lg font-semibold text-white mb-4">
        Discussion ({comments.length})
      </h3>

      {/* Comment form */}
      {isAuthenticated ? (
        <form onSubmit={handleSubmit} className="mb-6">
          <Textarea
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            placeholder="Share your thoughts..."
            rows={3}
          />
          <div className="mt-2 flex justify-end">
            <Button
              type="submit"
              size="sm"
              loading={submitting}
              disabled={!newComment.trim()}
            >
              Post Comment
            </Button>
          </div>
        </form>
      ) : (
        <div className="mb-6 p-4 bg-[#1a1a1a] rounded-lg text-center">
          <Link to="/login" className="text-[#3b82f6] hover:underline">
            Sign in to join the discussion
          </Link>
        </div>
      )}

      {/* Comments list */}
      {loading ? (
        <div className="flex justify-center py-8">
          <Spinner size="sm" />
        </div>
      ) : comments.length === 0 ? (
        <p className="text-[#666666] text-center py-8">No comments yet. Be the first!</p>
      ) : (
        <div className="space-y-4">
          {comments.map((comment) => (
            <div key={comment.uuid} className="p-4 bg-[#141414] rounded-lg">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 bg-[#3b82f6] rounded-full flex items-center justify-center">
                    <span className="text-xs font-medium text-white">
                      {comment.user?.name?.[0]?.toUpperCase() || '?'}
                    </span>
                  </div>
                  <span className="text-sm font-medium text-white">
                    {comment.user?.name || 'Anonymous'}
                  </span>
                </div>
                <span className="text-xs text-[#666666]">
                  {formatTimeAgo(comment.createdAt)}
                </span>
              </div>
              <p className="text-sm text-[#a0a0a0] pl-10">{comment.content}</p>
            </div>
          ))}
        </div>
      )}
    </Card>
  );
};

// ============================================================================
// MAIN EVENT DETAIL COMPONENT
// ============================================================================
const EventDetail = () => {
  const { id } = useParams();
  const { user, isAuthenticated } = useAuth();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    const fetchEvent = async () => {
      try {
        const response = await eventAPI.v1.getById(id);
        setEvent(response.data);
      } catch (err) {
        setError('Failed to load event');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchEvent();
  }, [id, refreshKey]);

  const handleOrderPlaced = () => {
    setRefreshKey((k) => k + 1);
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    );
  }

  if (error || !event) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-12 text-center">
        <p className="text-[#ef4444]">{error || 'Event not found'}</p>
        <Link to="/" className="text-[#3b82f6] hover:underline mt-4 inline-block">
          Back to markets
        </Link>
      </div>
    );
  }

  const market = event.markets?.[0];
  const yesProb = market?.lastPrice ? Math.round(market.lastPrice * 100) : 50;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      {/* Header */}
      <div className="mb-6">
        {/* Breadcrumb */}
        <nav className="mb-4">
          <Link to="/" className="text-[#888888] hover:text-white text-sm">
            Markets
          </Link>
          <span className="mx-2 text-[#555555]">/</span>
          <span className="text-white text-sm">{event.title}</span>
        </nav>

        {/* Title bar with icon and badge */}
        <div className="flex items-start gap-4">
          <div className={`w-14 h-14 bg-gradient-to-br ${getEventColor(event.uuid)} rounded-xl flex items-center justify-center shrink-0`}>
            <span className="text-xl font-bold text-white/40">
              {getInitials(event.title)}
            </span>
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-4">
              <h1 className="text-2xl font-bold text-white">{event.title}</h1>
              <Badge status={event.status} size="lg" className="shrink-0" />
            </div>
            {event.description && (
              <p className="text-[#888888] mt-1 line-clamp-2">{event.description}</p>
            )}
          </div>
        </div>
      </div>

      {/* Main Content - 2 column layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (2/3 width) */}
        <div className="lg:col-span-2 space-y-6">
          {/* Price Chart */}
          <PriceChart market={market} yesProb={yesProb} />

          {/* Order Book */}
          <OrderBook market={market} refreshKey={refreshKey} />

          {/* Market Stats */}
          <MarketStats event={event} market={market} />

          {/* Comments */}
          <CommentsSection eventId={event.uuid} />
        </div>

        {/* Right Column (1/3 width) - Sticky */}
        <div className="space-y-6">
          <div className="lg:sticky lg:top-20">
            {/* Trading Panel - Always show */}
            <TradingPanel
              market={market}
              event={event}
              onOrderPlaced={handleOrderPlaced}
            />

            {/* User Position */}
            {isAuthenticated && market && (
              <div className="mt-6">
                <UserPosition market={market} user={user} refreshKey={refreshKey} />
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default EventDetail;
