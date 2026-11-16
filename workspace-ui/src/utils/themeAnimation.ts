export function toggleThemeWithAnimation(
    event: MouseEvent,
    toggle: () => void,
) {
    const x = event.clientX;
    const y = event.clientY;
    const endRadius = Math.hypot(
        Math.max(x, window.innerWidth - x),
        Math.max(y, window.innerHeight - y),
    );

    document.documentElement.style.setProperty("--x", `${x}px`);
    document.documentElement.style.setProperty("--y", `${y}px`);
    document.documentElement.style.setProperty("--r", `${endRadius}px`);

    if (typeof document.startViewTransition === "function") {
        document.startViewTransition(() => toggle());
    } else {
        toggle();
    }
}
