import { useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectCart,
  setCartLoading,
  setCartSyncing,
  setCartError,
  setServerCart,
  clearReduxCart,
} from './cartSlice';
import { createCartService, CartItemRequest, StoreMismatchConflict } from './cartService';

export const useCart = (apiClient: any, onStoreMismatch?: (conflict: StoreMismatchConflict) => void) => {
  const dispatch = useDispatch();
  const cart = useSelector(selectCart);
  const cartService = createCartService(apiClient);

  /**
   * 1. Refresh Cart from Server
   * Must be called whenever the cart screen opens or user logs in.
   */
  const refreshCart = useCallback(async () => {
    dispatch(setCartLoading(true));
    try {
      const serverCart = await cartService.getCart();
      dispatch(setServerCart(serverCart));
      return serverCart;
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Failed to fetch cart';
      dispatch(setCartError(msg));
      throw err;
    } finally {
      dispatch(setCartLoading(false));
    }
  }, [dispatch, cartService]);

  /**
   * 2. Add Item to Cart
   * Dispatches POST /api/cart/items.
   * Redux state is updated ONLY AFTER the server returns 200 OK.
   */
  const addToCart = useCallback(
    async (productId: number, quantity: number = 1) => {
      dispatch(setCartLoading(true));
      try {
        const updatedCart = await cartService.addItem({ productId, quantity });
        dispatch(setServerCart(updatedCart));
        return { success: true, data: updatedCart };
      } catch (err: any) {
        // Handle Multi-Seller 409 Store Mismatch
        if (err.response?.status === 409 && err.response?.data?.error === 'STORE_MISMATCH') {
          if (onStoreMismatch) {
            onStoreMismatch(err.response.data);
          }
          return { success: false, conflict: err.response.data };
        }
        const msg = err.response?.data?.message || err.message || 'Failed to add item to cart';
        dispatch(setCartError(msg));
        throw err;
      } finally {
        dispatch(setCartLoading(false));
      }
    },
    [dispatch, cartService, onStoreMismatch]
  );

  /**
   * 3. Update Item Quantity
   * Dispatches PUT /api/cart/items/{id}.
   */
  const updateQuantity = useCallback(
    async (id: number, quantity: number) => {
      dispatch(setCartLoading(true));
      try {
        const updatedCart = await cartService.updateItemQuantity(id, quantity);
        dispatch(setServerCart(updatedCart));
        return updatedCart;
      } catch (err: any) {
        const msg = err.response?.data?.message || err.message || 'Failed to update quantity';
        dispatch(setCartError(msg));
        throw err;
      } finally {
        dispatch(setCartLoading(false));
      }
    },
    [dispatch, cartService]
  );

  /**
   * 4. Remove Item from Cart
   * Dispatches DELETE /api/cart/items/{id}.
   */
  const removeItem = useCallback(
    async (id: number) => {
      dispatch(setCartLoading(true));
      try {
        const updatedCart = await cartService.removeItem(id);
        dispatch(setServerCart(updatedCart));
        return updatedCart;
      } catch (err: any) {
        const msg = err.response?.data?.message || err.message || 'Failed to remove item';
        dispatch(setCartError(msg));
        throw err;
      } finally {
        dispatch(setCartLoading(false));
      }
    },
    [dispatch, cartService]
  );

  /**
   * 5. Post-Login Synchronization
   * Call immediately after successful Firebase authentication to sync guest items.
   */
  const syncGuestCartOnLogin = useCallback(
    async (guestItems: CartItemRequest[], targetStoreId?: number) => {
      dispatch(setCartSyncing(true));
      try {
        let syncedCart;
        if (guestItems && guestItems.length > 0) {
          syncedCart = await cartService.syncCart({ items: guestItems, targetStoreId });
        } else {
          syncedCart = await cartService.getCart();
        }
        dispatch(setServerCart(syncedCart));
        return syncedCart;
      } catch (err: any) {
        const msg = err.response?.data?.message || err.message || 'Failed to sync cart on login';
        dispatch(setCartError(msg));
        throw err;
      } finally {
        dispatch(setCartSyncing(false));
      }
    },
    [dispatch, cartService]
  );

  /**
   * 6. Pre-Checkout Server Cart Verification
   * Guarantees the server cart has items before allowing POST /api/orders.
   */
  const verifyServerCartBeforeCheckout = useCallback(async (): Promise<{
    canProceed: boolean;
    serverCart?: any;
    error?: string;
  }> => {
    try {
      const serverCart = await cartService.getCart();
      dispatch(setServerCart(serverCart));

      if (!serverCart.items || serverCart.items.length === 0) {
        return {
          canProceed: false,
          serverCart,
          error: 'Your cart is empty on the server. Please add items before placing an order.',
        };
      }

      return { canProceed: true, serverCart };
    } catch (err: any) {
      return {
        canProceed: false,
        error: err.response?.data?.message || 'Could not verify server cart.',
      };
    }
  }, [dispatch, cartService]);

  /**
   * 7. Clear Cart on Logout
   */
  const handleLogout = useCallback(() => {
    dispatch(clearReduxCart());
  }, [dispatch]);

  return {
    cart,
    refreshCart,
    addToCart,
    updateQuantity,
    removeItem,
    syncGuestCartOnLogin,
    verifyServerCartBeforeCheckout,
    handleLogout,
  };
};
