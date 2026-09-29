/**
 * Example Redux Store Configuration
 * Shows how to exclude 'cart' from redux-persist to make the backend
 * the single source of truth, while preserving auth session persistence.
 */

import { configureStore, combineReducers } from '@reduxjs/toolkit';
// import { persistStore, persistReducer } from 'redux-persist';
// import AsyncStorage from '@react-native-async-storage/async-storage'; // or storage for web
import cartReducer from './cartSlice';

const rootReducer = combineReducers({
  cart: cartReducer, // <--- CRITICAL: NOT wrapped in persistReducer!
  // auth: persistReducer(authPersistConfig, authReducer), // Auth remains persisted
});

/**
 * Persist config ONLY for auth / user preferences if needed.
 * DO NOT include 'cart' in persistConfig whitelist!
 */
export const store = configureStore({
  reducer: rootReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: false,
    }),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
