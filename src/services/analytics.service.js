import { db } from '../mock/db';
import { delay } from './api';

export const analyticsService = {
  async getAnalyticsData(params = '30d') {
    await delay(150);
    const timeRange = typeof params === 'string' ? params : params.timeRange || '30d';
    const startDate = typeof params === 'object' ? params.startDate : null;
    const endDate = typeof params === 'object' ? params.endDate : null;

    const data = db.getAnalytics();
    const orders = db.getOrders();
    const products = db.getProducts();
    const enquiries = db.getEnquiries();

    // Dynamically calculate KPIs
    const totalRevenue = orders.reduce((sum, o) => sum + (o.totalAmount || 0), 0);
    const pendingOrders = orders.filter(o => o.orderStatus === 'New' || o.orderStatus === 'Processing').length;
    const lowStockCount = products.filter(p => p.stock > 0 && p.stock <= p.lowStockThreshold).length;
    const outOfStockCount = products.filter(p => p.stock === 0).length;

    // Time-range specific trend data
    let revenueOverview = [];
    if (startDate && endDate) {
      // Dynamic intervals based on start and end date
      const start = new Date(startDate);
      const end = new Date(endDate);
      const diffDays = Math.max(1, Math.round((end - start) / (1000 * 60 * 60 * 24)));
      const steps = Math.min(8, Math.max(4, diffDays));
      const stepDays = Math.max(1, Math.floor(diffDays / steps));

      revenueOverview = Array.from({ length: steps }).map((_, idx) => {
        const d = new Date(start.getTime() + idx * stepDays * 24 * 60 * 60 * 1000);
        const label = d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
        const baseRev = 220000 + (idx * 45000) + ((idx % 2 === 0 ? 1 : -1) * 30000);
        return {
          label,
          revenue: baseRev,
          orders: Math.max(2, Math.round(baseRev / 45000)),
          enquiries: Math.max(4, Math.round(baseRev / 25000)),
        };
      });
    } else if (timeRange === '7d') {
      revenueOverview = [
        { label: 'Mon', revenue: 184500, orders: 4, enquiries: 8 },
        { label: 'Tue', revenue: 320000, orders: 7, enquiries: 12 },
        { label: 'Wed', revenue: 245000, orders: 5, enquiries: 9 },
        { label: 'Thu', revenue: 450000, orders: 9, enquiries: 16 },
        { label: 'Fri', revenue: 385000, orders: 8, enquiries: 14 },
        { label: 'Sat', revenue: 512000, orders: 11, enquiries: 19 },
        { label: 'Sun', revenue: 290000, orders: 6, enquiries: 10 },
      ];
    } else {
      // Default Custom 30-day window
      revenueOverview = [
        { label: '01 Aug', revenue: 1420000, orders: 22, enquiries: 38 },
        { label: '06 Aug', revenue: 1850000, orders: 28, enquiries: 46 },
        { label: '11 Aug', revenue: 2120000, orders: 34, enquiries: 55 },
        { label: '16 Aug', revenue: 1980000, orders: 30, enquiries: 49 },
        { label: '21 Aug', revenue: 2450000, orders: 38, enquiries: 62 },
        { label: '26 Aug', revenue: 2680000, orders: 42, enquiries: 68 },
        { label: '29 Aug', revenue: 2950000, orders: 48, enquiries: 74 },
      ];
    }

    return {
      ...data,
      revenueOverview,
      kpis: {
        totalRevenue: totalRevenue || 7728170,
        totalOrders: orders.length || 128,
        pendingOrders,
        totalProducts: products.length || 542,
        activeProducts: products.filter(p => p.status === 'Active').length || products.length,
        lowStockCount,
        outOfStockCount,
        totalEnquiries: enquiries.length || 8,
        newEnquiries: enquiries.filter(e => e.status === 'New').length || 2,
        conversionRate: 42.8,
        averageOrderValue: Math.round((totalRevenue || 7728170) / Math.max(1, orders.length || 128)),
      }
    };
  }
};
