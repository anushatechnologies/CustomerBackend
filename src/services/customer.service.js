import { apiClient, delay } from './api';
import { db } from '../mock/db';

export const customerService = {
  // GET /customers
  async getCustomers(params = {}) {
    await delay(150);
    const customers = db.getCustomers(params);
    return {
      data: customers,
      total: customers.length,
    };
  },

  // GET /customers/:id
  async getCustomerById(id) {
    await delay(100);
    const customer = db.getCustomerById(id);
    if (!customer) throw new Error(`Customer with ID ${id} not found`);
    return customer;
  },

  // POST /customers
  async createCustomer(customerData) {
    await delay(250);
    return db.createCustomer(customerData);
  },

  // PUT /customers/:id
  async updateCustomer(id, updates) {
    await delay(200);
    const updated = db.updateCustomer(id, updates);
    if (!updated) throw new Error(`Customer with ID ${id} not found`);
    return updated;
  },

  // PATCH /customers/:id/status
  async updateCustomerStatus(id, status) {
    await delay(150);
    const updated = db.updateCustomerStatus(id, status);
    if (!updated) throw new Error(`Customer with ID ${id} not found`);
    return updated;
  },

  // GET /customers/search?q=
  async searchCustomers(query = '') {
    await delay(100);
    return db.searchCustomers(query);
  },
};
