/**
 * Frontend Cart API Service
 * Interacts directly with Spring Boot CartController backend endpoints.
 */

export interface CartItemRequest {
  productId: number;
  quantity: number;
}

export interface CartItemUpdateRequest {
  quantity: number;
}

export interface CartSyncRequest {
  items: CartItemRequest[];
  targetStoreId?: number;
}

export interface ApiResponse<T> {
  success: boolean;
  statusCode: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface StoreMismatchConflict {
  status: 409;
  error: 'STORE_MISMATCH';
  message: string;
  currentStore: { id: number; name: string; slug: string };
  newStore: { id: number; name: string; slug: string };
}

// In production, apiClient includes Authorization: Bearer <FirebaseIdToken>
export const createCartService = (apiClient: any) => ({
  /**
   * GET /api/cart
   * Fetches latest authoritative cart state from MySQL database.
   */
  getCart: async (): Promise<any> => {
    const response = await apiClient.get('/api/cart');
    return response.data.data;
  },

  /**
   * POST /api/cart/items
   * Adds or sets product in user's active cart.
   */
  addItem: async (request: CartItemRequest): Promise<any> => {
    const response = await apiClient.post('/api/cart/items', request);
    return response.data.data;
  },

  /**
   * PUT /api/cart/items/{id}
   * Updates quantity of cart item (or deletes if quantity = 0).
   * Primary: cartItemId; Fallback: productId.
   */
  updateItemQuantity: async (id: number, quantity: number): Promise<any> => {
    const response = await apiClient.put(`/api/cart/items/${id}`, { quantity });
    return response.data.data;
  },

  /**
   * DELETE /api/cart/items/{id}
   * Removes item from cart by cartItemId (or productId).
   */
  removeItem: async (id: number): Promise<any> => {
    const response = await apiClient.delete(`/api/cart/items/${id}`);
    return response.data.data;
  },

  /**
   * POST /api/cart/sync
   * Atomically merges guest cart items into authenticated user's database cart upon login.
   */
  syncCart: async (request: CartSyncRequest): Promise<any> => {
    const response = await apiClient.post('/api/cart/sync', request);
    return response.data.data;
  },

  /**
   * DELETE /api/cart
   * Clears the active cart.
   */
  clearCart: async (): Promise<void> => {
    await apiClient.delete('/api/cart');
  },

  /**
   * POST /api/cart/switch-store
   * Switches active store cart when 409 STORE_MISMATCH occurs.
   */
  switchStore: async (storeId: number, pendingProductId?: number, pendingQuantity?: number): Promise<any> => {
    const response = await apiClient.post('/api/cart/switch-store', {
      storeId,
      pendingProductId,
      pendingQuantity,
    });
    return response.data.data;
  },
});
