import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, test, vi } from "vitest";
import { NewGoalForm } from "./new-goal-form";

const router = {
  push: vi.fn(),
  refresh: vi.fn(),
};

vi.mock("next/navigation", () => ({
  useRouter: () => router,
}));

describe("NewGoalForm", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    router.push.mockReset();
    router.refresh.mockReset();
  });

  test("shows validation errors for an incomplete goal", async () => {
    const user = userEvent.setup();
    render(<NewGoalForm />);

    await user.click(screen.getByRole("button", { name: /create goal/i }));

    expect(
      await screen.findByText(
        "Describe your background in at least 10 characters.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "Describe your learning goal in at least 10 characters.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Enter a target role or career direction."),
    ).toBeInTheDocument();
  });

  test("surfaces backend API errors without navigating", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        json: async () => ({
          errors: { title: "A similar active goal already exists." },
          message: "Validation failed.",
        }),
        ok: false,
      }),
    );
    render(<NewGoalForm />);

    await user.type(
      screen.getByLabelText(/technical background/i),
      "Java backend developer with PostgreSQL experience.",
    );
    await user.type(
      screen.getByLabelText(/learning goal/i),
      "Build production-ready AI agent applications.",
    );
    await user.type(screen.getByLabelText(/job target/i), "AI Engineer");
    await user.click(screen.getByRole("button", { name: /create goal/i }));

    expect(
      await screen.findByText("Unable to create goal."),
    ).toBeInTheDocument();
    expect(
      screen.getByText("A similar active goal already exists."),
    ).toBeInTheDocument();
    expect(router.push).not.toHaveBeenCalled();
  });
});
