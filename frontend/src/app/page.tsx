export default function Home() {
  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground">
      <section className="mx-auto flex w-full max-w-5xl flex-col gap-8">
        <header className="flex flex-col gap-3 border-b border-slate-200 pb-6">
          <p className="text-sm font-medium text-slate-500">Day 04</p>
          <h1 className="text-3xl font-semibold text-slate-950">
            AI Developer Learning Planner
          </h1>
          <p className="max-w-2xl text-base leading-7 text-slate-600">
            Frontend workspace initialized with Next.js, TypeScript, Tailwind
            CSS, and ESLint.
          </p>
        </header>

        <div className="grid gap-4 md:grid-cols-3">
          {[
            "Create learning goals",
            "Review generated plans",
            "Track daily tasks",
          ].map((item) => (
            <article
              className="rounded-md border border-slate-200 bg-white p-5 shadow-sm"
              key={item}
            >
              <h2 className="text-base font-semibold text-slate-950">{item}</h2>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                Reserved for the MVP workflow.
              </p>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
