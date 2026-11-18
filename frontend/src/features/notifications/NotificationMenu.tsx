import { useState } from "react";
import { Bell, CheckCheck } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { ErrorState, Loading } from "../../components/ui";
import { useAuth } from "../auth/useAuth";
import { useNotificationActions, useNotifications, useUnreadNotificationCount } from "./api";

const createdLabel = (value: string) => new Intl.DateTimeFormat("pl-PL", {
  dateStyle: "medium", timeStyle: "short",
}).format(new Date(value));

export function NotificationMenu() {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const [page, setPage] = useState(0);
  const navigate = useNavigate();
  const unread = useUnreadNotificationCount(user?.id);
  const notifications = useNotifications(user?.id, open, page);
  const actions = useNotificationActions(user?.id);

  const visit = async (notification: NonNullable<typeof notifications.data>["content"][number]) => {
    if (!notification.read) {
      try {
        await actions.markRead.mutateAsync(notification.id);
      } catch {
        return;
      }
    }
    setOpen(false);
    navigate(`/reminders/${notification.vehicleId}/${notification.reminderId}`);
  };

  if (!user) return null;
  return (
    <div className="notification-menu">
      <button className="icon-button notification-trigger" type="button" aria-label="Powiadomienia"
        aria-expanded={open} aria-controls="notification-panel" onClick={() => { setPage(0); setOpen(!open); }}>
        <Bell size={19} />
        {unread.data ? <span className="notification-count" aria-label={`${unread.data} nieprzeczytanych`}>{unread.data > 99 ? "99+" : unread.data}</span> : null}
      </button>
      {open && <section id="notification-panel" className="notification-panel" aria-label="Powiadomienia">
        <div className="notification-panel-heading">
          <div><h2>Powiadomienia</h2><span>{unread.data ?? 0} nieprzeczytanych</span></div>
          <button className="notification-mark-all" type="button" disabled={!unread.data || actions.markAllRead.isPending}
            onClick={() => actions.markAllRead.mutate()}><CheckCheck size={15} /> Odczytaj wszystkie</button>
        </div>
        {unread.isError ? <ErrorState error={unread.error} retry={() => void unread.refetch()} /> : null}
        {actions.markRead.isError || actions.markAllRead.isError ? <ErrorState error={actions.markRead.error ?? actions.markAllRead.error!} /> : null}
        {notifications.isPending ? <Loading /> : notifications.isError ? <ErrorState error={notifications.error} retry={() => void notifications.refetch()} />
          : notifications.data.content.length === 0 ? <div className="notification-empty"><Bell size={22} /><strong>Brak powiadomień</strong><span>Nowe przypomnienia pojawią się tutaj.</span></div>
          : <div className="notification-list">{notifications.data.content.map(item =>
            <button className={`notification-item${item.read ? " is-read" : " is-unread"}`} type="button" key={item.id}
              onClick={() => void visit(item)}>
              <span className="notification-item-title">{item.title}<span className={`notification-read-state ${item.read ? "" : "unread-dot"}`}>{item.read ? "Przeczytane" : "Nieprzeczytane"}</span></span>
              <span>{item.message}</span><span className="notification-vehicle">{item.vehicleName}</span>
              <time dateTime={item.createdAt}>{createdLabel(item.createdAt)}</time>
            </button>)}
          </div>}
        {!notifications.isPending && !notifications.isError && notifications.data.totalPages > 1 && <div className="notification-pagination">
          <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>Nowsze</button>
          <span>{page + 1} / {notifications.data.totalPages}</span>
          <button type="button" disabled={page + 1 >= notifications.data.totalPages} onClick={() => setPage(page + 1)}>Starsze</button>
        </div>}
      </section>}
    </div>
  );
}
