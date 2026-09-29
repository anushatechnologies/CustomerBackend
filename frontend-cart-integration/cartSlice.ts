import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export interface CartItem {
  cartItemId: number;
  productId: number;
  title: string;
  imageUrl?: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  originalPrice: number;
  appliedTier?: string;
  gstRate: number;
  lineTotal: number;
  lineGst: number;
}

export interface CartState {
  cartId: number | null;
  storeId: number | null;
  storeName: string | null;
  storeSlug: string | null;
  items: CartItem[];
  subtotal: number;
  couponDiscount: number;
  totalGst: number;
  deliveryCharge: number;
  grandTotal: number;
  appliedCoupon?: string | null;
  loading: boolean;
  syncing: boolean;
  error: string | null;
}

const initialState: CartState = {
  cartId: null,
  storeId: null,
  storeName: null,
  storeSlug: null,
  items: [],
  subtotal: 0,
  couponDiscount: 0,
  totalGst: 0,
  deliveryCharge: 0,
  grandTotal: 0,
  appliedCoupon: null,
  loading: false,
  syncing: false,
  error: null,
};

/**
 * Cart slice strictly maintains UI/cache representation of the backend database cart.
 * It is NOT persisted to AsyncStorage/localStorage to guarantee zero frontend/backend divergence.
 */
export const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    setCartLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setCartSyncing: (state, action: PayloadAction<boolean>) => {
      state.syncing = action.payload;
    },
    setCartError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    // Invoked exclusively after receiving a 200 OK from GET /api/cart, POST /api/cart/items, etc.
    setServerCart: (state, action: PayloadAction<any>) => {
      const data = action.payload;
      state.cartId = data.cartId ?? null;
      state.storeId = data.storeId ?? null;
      state.storeName = data.storeName ?? null;
      state.storeSlug = data.storeSlug ?? null;
      state.items = data.items || [];
      state.subtotal = data.subtotal ?? 0;
      state.couponDiscount = data.couponDiscount ?? 0;
      state.totalGst = data.totalGst ?? 0;
      state.deliveryCharge = data.deliveryCharge ?? 0;
      state.grandTotal = data.grandTotal ?? 0;
      state.appliedCoupon = data.appliedCoupon ?? null;
      state.loading = false;
      state.syncing = false;
      state.error = null;
    },
    // Clears Redux cart on logout or upon order placement
    clearReduxCart: (state) => {
      return { ...initialState };
    },
  },
});

export const { setCartLoading, setCartSyncing, setCartError, setServerCart, clearReduxCart } = cartSlice.actions;

export const selectCart = (state: { cart: CartState }) => state.cart;
export const selectCartItems = (state: { cart: CartState }) => state.cart.items;
export const selectCartTotal = (state: { cart: CartState }) => state.cart.grandTotal;
export const selectCartItemCount = (state: { cart: CartState }) =>
  state.cart.items.reduce((acc, item) => acc + item.quantity, 0);

export default cartSlice.reducer;
