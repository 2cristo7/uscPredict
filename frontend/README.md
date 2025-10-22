# USC Predict - Frontend

Simple React frontend for USC Predict prediction market platform.

## Tech Stack

- **React** 18 with Vite
- **Tailwind CSS** for styling
- **Axios** for HTTP requests

## Prerequisites

- Node.js 18+ and npm
- Backend running on `http://localhost:8080`

## Installation

```bash
npm install
```

## Running the Application

```bash
npm run dev
```

The application will be available at `http://localhost:5173`

## Features

### 1. Users Management
- Create new users
- View all users
- Delete users
- Create wallets for users

### 2. Wallets Management
- View all wallets
- Deposit funds
- Withdraw funds
- See available and locked balances

### 3. Events & Markets
- Create prediction events
- Create markets for events
- Match orders manually
- Settle markets

### 4. Orders & Positions
- Place BUY orders (YES shares)
- Place SELL orders (NO shares)
- View order book
- Cancel pending orders
- View user positions (YES/NO shares)
- See net exposure

## How to Use

### 1. Create Users and Wallets

1. Go to "Users" tab
2. Click "Create User" and fill in the form
3. Once user is created, click "Create Wallet" to give them initial funds ($1000)

### 2. Create an Event and Market

1. Go to "Markets & Events" tab
2. Click "Create Event" and enter event details (e.g., "Will it rain tomorrow?")
3. Click "Create Market" and select the event
4. Enter outcome (typically "YES") and create market

### 3. Place Orders

1. Go to "Orders & Positions" tab
2. Click "Place Order"
3. Select user, market, side (BUY for YES, SELL for NO), price (0-1), and quantity
4. **BUY** = Betting on YES (pays `price × quantity`)
5. **SELL** = Betting on NO (pays `(1 - price) × quantity`)

### 4. View Results

- Orders will automatically match if compatible
- Check "Positions" table to see user holdings
- **YES Shares**: Number of YES outcome shares
- **NO Shares**: Number of NO outcome shares
- **Net Exposure**: Positive = bullish, Negative = bearish

## API Endpoints Used

All endpoints connect to `http://localhost:8080`:

- `/users` - User management
- `/wallets` - Wallet operations
- `/events` - Event management
- `/markets` - Market management
- `/orders` - Order placement and management
- `/positions` - Position tracking
- `/transactions` - Transaction history

## Notes

- CORS is enabled in the backend for `localhost:5173` and `localhost:3000`
- All operations are performed in real-time
- No authentication required (development mode)
- Data persists in PostgreSQL database

## Development

To modify the frontend:

1. Components are in `src/components/`
2. API service is in `src/services/api.js`
3. Main app layout is in `src/App.jsx`
4. Tailwind configuration is in `tailwind.config.js`
