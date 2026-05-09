import { useParams } from 'react-router-dom';

export function TicketDetailPage() {
  const { ticketId } = useParams();

  return (
    <section>
      <h1>Ticket Detail</h1>
      <p className="muted-text">
        Detailed ticket workflow UI will be implemented in the next iteration.
      </p>
      <p>
        Current ticket id: <strong>{ticketId}</strong>
      </p>
    </section>
  );
}
