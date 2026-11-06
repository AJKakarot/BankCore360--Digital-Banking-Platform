"use client";

import { cn } from "@/lib/utils";
import React, { useEffect, useState } from "react";

export const InfiniteMovingCards = ({
  items,
  direction = "left",
  speed = "normal",
  pauseOnHover = true,
  className,
}: {
  items: {
    quote: string;
    name: string;
    title: string;
    category?: string;
  }[];
  direction?: "left" | "right";
  speed?: "fast" | "normal" | "slow";
  pauseOnHover?: boolean;
  className?: string;
}) => {
  const containerRef = React.useRef<HTMLDivElement>(null);
  const scrollerRef = React.useRef<HTMLUListElement>(null);

  useEffect(() => {
    addAnimation();
  }, []);

  const [start, setStart] = useState(false);

  function addAnimation() {
    if (containerRef.current && scrollerRef.current) {
      const scrollerContent = Array.from(scrollerRef.current.children);

      scrollerContent.forEach((item) => {
        const duplicatedItem = item.cloneNode(true);
        if (scrollerRef.current) {
          scrollerRef.current.appendChild(duplicatedItem);
        }
      });

      getDirection();
      getSpeed();
      setStart(true);
    }
  }

  const getDirection = () => {
    if (containerRef.current) {
      if (direction === "left") {
        containerRef.current.style.setProperty(
          "--animation-direction",
          "forwards"
        );
      } else {
        containerRef.current.style.setProperty(
          "--animation-direction",
          "reverse"
        );
      }
    }
  };

  const getSpeed = () => {
    if (containerRef.current) {
      if (speed === "fast") {
        containerRef.current.style.setProperty("--animation-duration", "35s");
      } else if (speed === "normal") {
        containerRef.current.style.setProperty("--animation-duration", "65s");
      } else {
        containerRef.current.style.setProperty("--animation-duration", "95s");
      }
    }
  };

  return (
    <div
      ref={containerRef}
      className={cn(
        "scroller relative z-20 max-w-7xl overflow-hidden [mask-image:linear-gradient(to_right,transparent,white_8%,white_92%,transparent)]",
        className
      )}
    >
      <ul
        ref={scrollerRef}
        className={cn(
          "flex min-w-full shrink-0 gap-5 py-4 w-max flex-nowrap",
          start && "animate-scroll",
          pauseOnHover && "hover:[animation-play-state:paused]"
        )}
      >
        {items.map((item, idx) => (
          <li
            key={`${item.name}-${idx}`}
            className="w-[270px] sm:w-[300px] md:w-[320px] max-w-full relative rounded-2xl border shrink-0 border-[#262626] bg-[#1C1C1C] hover:bg-[#202020] hover:border-[#CAFF33] px-6 py-5 transition-all duration-300 shadow-lg hover:shadow-[0_0_20px_rgba(202,255,51,0.15)] hover:scale-[1.01]"
          >
            <blockquote>
              <div
                aria-hidden="true"
                className="user-select-none -z-1 pointer-events-none absolute -left-0.5 -top-0.5 h-[calc(100%_+_4px)] w-[calc(100%_+_4px)]"
              ></div>
              <div className="relative z-20">
                <span className="text-[#CAFF33] text-3xl font-serif leading-none select-none block mb-1">“</span>
                <p className="text-xs leading-relaxed text-neutral-300 font-normal min-h-[60px]">
                  {item.quote}
                </p>
              </div>
              <div className="relative z-20 mt-4 pt-3 border-t border-[#262626] flex items-center justify-between">
                <span className="flex flex-col gap-0.5">
                  <span className="text-xs leading-tight text-[#CAFF33] font-bold">
                    {item.name}
                  </span>
                  <span className="text-[10px] leading-tight text-neutral-500 font-normal">
                    {item.title}
                  </span>
                </span>
                {item.category && (
                  <span className="text-[9px] font-bold px-2 py-0.5 rounded-full bg-[#141414] border border-[#262626] text-neutral-400">
                    {item.category}
                  </span>
                )}
              </div>
            </blockquote>
          </li>
        ))}
      </ul>
    </div>
  );
};

