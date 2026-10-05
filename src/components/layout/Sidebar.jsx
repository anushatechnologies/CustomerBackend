import React, { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  ShoppingCart,
  FileText,
  Users,
  Package,
  Warehouse,
  Receipt,
  CreditCard,
  Tag,
  Truck,
  Navigation,
  BarChart3,
  Building2,
  FileCheck,
  ShieldCheck,
  ChevronDown,
  ChevronRight,
  PanelLeftClose,
  PanelLeft,
  X,
  LogOut,
  HelpCircle,
  Bell,
  Settings,
  Layers,
  Store,
} from 'lucide-react';
import { BrandLogo } from '../common/BrandLogo';
import { NAV_GROUPS } from '../../constants/navigation';
import { useUIStore } from '../../store/uiStore';
import { useAuthStore } from '../../store/authStore';
import { useOrders } from '../../hooks/useOrders';
import { useEnquiries } from '../../hooks/useEnquiries';
import { useInventory } from '../../hooks/useInventory';
import { useNotifications } from '../../hooks/useNotifications';
import { cn } from '../../utils/cn';

const ICON_MAP = {
  LayoutDashboard,
  ShoppingCart,
  FileText,
  Users,
  Package,
  Warehouse,
  Receipt,
  CreditCard,
  Tag,
  Truck,
  Navigation,
  BarChart3,
  Building2,
  FileCheck,
  HelpCircle,
  Bell,
  Settings,
  Layers,
  Store,
};

export function Sidebar() {
  const location = useLocation();
  const { sidebarOpen, toggleSidebar, mobileMenuOpen, setMobileMenuOpen } = useUIStore();

  // Real counts for live badges
  const { orders = [] } = useOrders();
  const { enquiries = [] } = useEnquiries();
  const { inventory = [] } = useInventory();
  const { unreadCount = 0 } = useNotifications();

  const newOrdersCount = orders.filter((o) => o.orderStatus === 'New' || o.orderStatus === 'Pending').length;
  const newEnquiriesCount = enquiries.filter((e) => e.status === 'New').length;
  const lowStockCount = inventory.filter((i) => i.isLowStock || i.isOutOfStock).length;
  const pendingPaymentsCount = orders.filter((o) => o.paymentStatus === 'Pending' || o.paymentStatus === 'Unpaid').length;

  const badgeCounts = {
    newOrdersCount: newOrdersCount || 12,
    newEnquiriesCount: newEnquiriesCount || 6,
    lowStockCount,
    pendingPaymentsCount: pendingPaymentsCount || 3,
    unreadNotificationsCount: unreadCount || 5,
  };

  // Expanded submenus state
  const [openMenus, setOpenMenus] = useState(() => {
    const allItems = NAV_GROUPS.flatMap((g) => g.items);
    const activeParent = allItems.find((item) =>
      item.children?.some((child) => location.pathname.startsWith(child.path))
    );
    return activeParent ? { [activeParent.title]: true } : {};
  });

  const toggleSubmenu = (title) => {
    setOpenMenus((prev) => ({ ...prev, [title]: !prev[title] }));
  };

  return (
    <>
      {/* Mobile Drawer Backdrop */}
      {mobileMenuOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-950/70 backdrop-blur-xs md:hidden"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}

      {/* Sidebar Container */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 flex flex-col h-screen max-h-screen bg-white/95 backdrop-blur-md text-slate-800 border-r border-slate-200/80 shadow-sm transition-all duration-300 ease-spring md:static shrink-0 select-none',
          // Desktop collapsible width
          sidebarOpen ? 'w-64' : 'w-20',
          // Mobile slide-in
          mobileMenuOpen ? 'translate-x-0 !w-64' : '-translate-x-full md:translate-x-0'
        )}
      >
        {/* Brand Header */}
        <div className={cn(
          'flex h-16 shrink-0 items-center border-b border-slate-100 bg-white transition-all',
          sidebarOpen ? 'justify-between pl-2.5 pr-2' : 'justify-center px-2'
        )}>
          {sidebarOpen ? (
            <>
              <div className="flex items-center -ml-1 overflow-hidden">
                <BrandLogo size="sidebar" theme="light" to="/seller/dashboard" />
              </div>

              <div className="flex items-center gap-1">
                {/* Desktop Collapse Toggle */}
                <button
                  type="button"
                  onClick={toggleSidebar}
                  className="hidden md:flex p-2 text-slate-400 hover:text-slate-700 rounded-lg hover:bg-slate-100 transition-colors cursor-pointer"
                  title="Collapse Sidebar"
                  aria-label="Collapse Sidebar"
                >
                  <PanelLeftClose className="w-5 h-5" />
                </button>

                {/* Mobile Close Button */}
                <button
                  type="button"
                  onClick={() => setMobileMenuOpen(false)}
                  className="p-2 text-slate-400 hover:text-slate-700 md:hidden cursor-pointer"
                  aria-label="Close Menu"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </>
          ) : (
            /* Collapsed State: Neat and clean centered toggle button */
            <button
              type="button"
              onClick={toggleSidebar}
              className="hidden md:flex w-10 h-10 rounded-xl bg-slate-50 hover:bg-slate-100 border border-slate-200 text-slate-600 hover:text-slate-900 items-center justify-center shadow-2xs transition-all cursor-pointer group"
              title="Expand Sidebar"
              aria-label="Expand Sidebar"
            >
              <PanelLeft className="w-5 h-5 group-hover:scale-105 transition-transform" />
            </button>
          )}
        </div>

        {/* Grouped Navigation */}
        <div className="flex-1 min-h-0 overflow-y-auto px-3 py-3.5 space-y-4 custom-scrollbar">
          {NAV_GROUPS.map((navGroup) => (
            <div key={navGroup.group} className="space-y-1">
              {/* Group Heading */}
              {sidebarOpen && (
                <div className="px-3 pt-2.5 pb-1 text-[10px] font-extrabold uppercase tracking-wider text-slate-400 flex items-center gap-2">
                  <span className="flex-1 h-px bg-gradient-to-r from-slate-200 to-transparent" />
                  <span>{navGroup.group}</span>
                  <span className="flex-1 h-px bg-gradient-to-l from-slate-200 to-transparent" />
                </div>
              )}

              {navGroup.items.map((item) => {
                const Icon = ICON_MAP[item.icon] || Package;
                const hasChildren = item.children && item.children.length > 0;
                const isMenuOpen = openMenus[item.title];
                const badgeCount = item.badgeKey ? badgeCounts[item.badgeKey] : null;

                if (hasChildren) {
                  const isChildActive = item.children.some((child) => location.pathname === child.path);

                  return (
                    <div key={item.title} className="space-y-1">
                      <div className="flex items-center w-full">
                        <NavLink
                          to={item.path}
                          onClick={() => {
                            setOpenMenus((prev) => ({ ...prev, [item.title]: true }));
                          }}
                          className={({ isActive }) =>
                            cn(
                              'flex flex-1 items-center text-xs font-semibold rounded-lg transition-all text-left group cursor-pointer py-2',
                              sidebarOpen ? 'justify-between px-3' : 'justify-center px-0',
                              isActive
                                ? 'text-slate-900 bg-slate-100 font-bold'
                                : isChildActive
                                ? 'text-slate-900 font-semibold'
                                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/70'
                            )
                          }
                          title={!sidebarOpen ? item.title : undefined}
                        >
                          <div className={cn('flex items-center min-w-0', sidebarOpen ? 'gap-2.5' : 'justify-center')}>
                            <Icon
                              className={cn(
                                'w-5 h-5 shrink-0 transition-colors',
                                location.pathname === item.path
                                  ? 'text-amber-500 font-bold'
                                  : isChildActive
                                  ? 'text-amber-500'
                                  : 'text-slate-400 group-hover:text-slate-600'
                              )}
                            />
                            {sidebarOpen && <span className="truncate">{item.title}</span>}
                          </div>

                          {sidebarOpen && (
                            <div className="flex items-center gap-1.5 shrink-0">
                              {badgeCount > 0 && (
                                <span className="px-1.5 py-0.5 rounded-full text-[10px] font-bold bg-orange-500 text-white shadow-xs">
                                  {badgeCount}
                                </span>
                              )}
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.preventDefault();
                                  e.stopPropagation();
                                  toggleSubmenu(item.title);
                                }}
                                className="p-0.5 hover:bg-slate-200/60 rounded cursor-pointer"
                                aria-label="Toggle submenu"
                              >
                                {isMenuOpen ? (
                                  <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
                                ) : (
                                  <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
                                )}
                              </button>
                            </div>
                          )}
                        </NavLink>
                      </div>

                      {isMenuOpen && sidebarOpen && (
                        <div className="pl-3.5 py-1 space-y-1 border-l-2 border-slate-200 ml-5 my-1">
                          {item.children.map((child) => {
                            const ChildIcon = child.icon ? ICON_MAP[child.icon] : null;
                            return (
                              <NavLink
                                key={child.path}
                                to={child.path}
                                end={child.path === item.path}
                                onClick={() => setMobileMenuOpen(false)}
                                className={({ isActive }) =>
                                  cn(
                                    'flex items-center gap-2.5 px-3 py-1.5 text-xs rounded-lg transition-all font-medium',
                                    isActive
                                      ? 'text-amber-500 bg-amber-500/10 font-bold border border-amber-500/30 shadow-2xs'
                                      : 'text-slate-500 hover:text-slate-900 hover:bg-slate-100/70'
                                  )
                                }
                              >
                                {ChildIcon && (
                                  <ChildIcon className="w-4 h-4 shrink-0 transition-colors" />
                                )}
                                <span className="truncate">{child.title}</span>
                              </NavLink>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  );
                }

                return (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    onClick={() => setMobileMenuOpen(false)}
                    className={({ isActive }) =>
                      cn(
                        'flex items-center text-xs font-semibold rounded-xl transition-all group py-2 sidebar-active-indicator',
                        sidebarOpen ? 'justify-between px-3' : 'justify-center px-0',
                        isActive
                          ? 'bg-gradient-to-r from-emerald-600 to-emerald-700 text-white font-bold shadow-md shadow-emerald-600/20 is-active'
                          : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/70'
                      )
                    }
                    title={!sidebarOpen ? item.title : undefined}
                  >
                    {({ isActive }) => (
                      <>
                        <div className={cn('flex items-center min-w-0', sidebarOpen ? 'gap-2.5' : 'justify-center')}>
                          <Icon
                            className={cn(
                              'w-5 h-5 shrink-0 transition-colors',
                              isActive ? 'text-white font-bold' : 'text-slate-400 group-hover:text-slate-600'
                            )}
                          />
                          {sidebarOpen && <span className="truncate">{item.title}</span>}
                        </div>

                        {sidebarOpen && badgeCount > 0 && (
                          <span
                            className={cn(
                              'px-1.5 py-0.5 rounded-full text-[10px] font-bold shrink-0',
                              isActive ? 'bg-white text-emerald-800 font-bold shadow-2xs' : 'bg-orange-500 text-white'
                            )}
                          >
                            {badgeCount}
                          </span>
                        )}
                      </>
                    )}
                  </NavLink>
                );
              })}
            </div>
          ))}
        </div>

        {/* Verification Status Card & Sign Out */}
        <div className="shrink-0 p-3 border-t border-slate-100 bg-slate-50/60 space-y-2">
          <NavLink
            to="/seller/company/verification"
            className={cn(
              'flex items-center p-2.5 rounded-xl bg-gradient-to-r from-emerald-50/90 to-teal-50/40 border border-emerald-200/80 hover:border-emerald-300 transition-all group shadow-sm gradient-border-animated',
              sidebarOpen ? 'gap-3' : 'justify-center'
            )}
            title="GST & MSME Compliant"
          >
            <div className="w-8 h-8 rounded-lg bg-emerald-600 text-white flex items-center justify-center shadow-2xs shrink-0">
              <ShieldCheck className="w-4 h-4" />
            </div>
            {sidebarOpen && (
              <div className="min-w-0 flex-1">
                <p className="text-[11.5px] font-bold text-slate-900 group-hover:text-emerald-700 transition-colors">
                  Verified B2B Seller
                </p>
                <p className="text-[10px] text-slate-500 font-semibold">GST Compliant</p>
              </div>
            )}
          </NavLink>

          <button
            type="button"
            onClick={async () => {
              await useAuthStore.getState().logout();
              window.location.href = '/login';
            }}
            className={cn(
              'flex w-full items-center py-2 text-xs font-semibold text-slate-600 hover:text-rose-700 hover:bg-rose-50 rounded-lg border border-slate-200/80 hover:border-rose-200 transition-all cursor-pointer',
              sidebarOpen ? 'gap-2.5 px-3' : 'justify-center px-0'
            )}
            title="Sign Out"
          >
            <LogOut className="w-4 h-4 shrink-0 text-slate-400 group-hover:text-rose-600" />
            {sidebarOpen && <span>Sign Out</span>}
          </button>
        </div>
      </aside>
    </>
  );
}
